@Library('my_shared_library')_

def workspace;
def branch;
def dockerImage;
def props='';
def microserviceName;
def port;
def docImg;
def repoName;
def credentials = 'docker-credentials';

node {
    stage('Checkout Code')
    {
	checkout scm
	workspace = pwd() 
	     sh "ls -lat"
	props = readProperties  file: """deploy.properties"""   
    }
    
    /*stage ('Check-secrets')
    {
	sh "rm trufflehog || true"
	sh "docker run gesellix/trufflehog --json ${props['deploy.gitURL']} > trufflehog"
	sh "cat trufflehog"
    }
    
    stage ('Source Composition Analysis') 
    {
         sh 'rm owasp* || true'
         sh 'wget "https://raw.githubusercontent.com/Devops-Accelerators/Micro/master/owasp-dependency-check.sh" '
         sh 'chmod +x owasp-dependency-check.sh'
         sh 'bash owasp-dependency-check.sh'
         sh 'cat /var/lib/jenkins/OWASP-Dependency-Check/reports/dependency-check-report.xml'
    }*/
    
    stage ('create war')
    {
    	mavenbuildexec "mvn build"
    }
    
    stage ('Create Docker Image')
    { 
	     echo 'creating an image'
	     docImg="${props['deploy.dockerhub']}/${props['deploy.microservice']}"
             dockerImage = dockerexec "${docImg}"
    }
    
     stage ('Push Image to Docker Registry')
    { 
	     docker.withRegistry('https://registry.hub.docker.com','docker-credentials') {
             dockerImage.push("${BUILD_NUMBER}")
	     }
    }
    
    stage ('Config helm')
    { 
    	
	def filename = 'helmchart/values.yaml'
	def data = readYaml file: filename
	
	data.image.repository = "${docImg}"
	data.image.tag = "$BUILD_NUMBER"
	data.service.appPort = "${props['deploy.port']}"
	
	sh "rm -f helmchart/values.yaml"
	writeYaml file: filename, data: data
	
    }
    stage ('deploy to cluster')
    {
    	//helmdeploy "${props['deploy.microservice']}"
	withKubeConfig(credentialsId: 'kubernetes-creds', serverUrl: 'https://35.225.27.58') {

		sh """ helm delete --purge ${props['deploy.microservice']} | true"""
		helmdeploy "${props['deploy.microservice']}"
	}
	
    }
    
    stage ('scan-kubernetes')
    {
    	withKubeConfig(credentialsId: 'kubernetes-creds', serverUrl: 'https://35.225.27.58') 
	{
		
		sh """
		rm tiocsscanner* || true
		cat >> tiocsscanner-namespace.yaml <<EOF
apiVersion: v1
kind: Namespace
metadata: 
  name: tiocsscanner
  labels: 
    name: tiocsscanner"""
    
    		sh "kubectl apply -f tiocsscanner-namespace.yaml"
		
		sh """kubectl create secret generic tio --from-literal=username=$TENABLE_ACCESS_KEY \
--from-literal=password=$TENABLE_SECRET_KEY --namespace=tiocsscanner"""
		
		sh """
		cat >> tiocsscanner-deployment.yaml <<EOF
apiVersion: v1
kind: Service
metadata:
  name: tiocsscanner
  namespace: tiocsscanner
  labels:
    app: tiocsscanner
spec:
  selector:
    app: tiocsscanner
  type: ClusterIP
  ports:
  - name: http
    protocol: TCP
    port: 5000
--- 
apiVersion: extensions/v1beta1
kind: Deployment
metadata: 
  labels: 
    app: tiocsscanner
  name: tiocsscanner
  namespace: tiocsscanner
spec: 
  minReadySeconds: 10
  replicas: 1
  selector: 
    matchLabels: 
      app: tiocsscanner
  strategy: 
    rollingUpdate: 
      maxSurge: 1
      maxUnavailable: 1
    type: RollingUpdate
  template: 
    metadata: 
      labels: 
        app: tiocsscanner
    spec: 
      containers: 
        - image: "tenableio-docker-consec-local.jfrog.io/cs-scanner:latest"
          name: tiocsscanner
          resources: 
            limits:
              cpu: "3"
            requests: 
              cpu: "1.5"
              memory: "2Gi"
          args:
           - import-registry							
          env:
            - name: TENABLE_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: tio
                  key: username
            - name: TENABLE_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: tio
                  key: password
            - name: REGISTRY_USERNAME
              valueFrom:
                secretKeyRef:
                  name: private-registry
                  key: username
            - name: REGISTRY_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: private-registry
                  key: password
            - name: IMPORT_REPO_NAME
              value: "<variable>"
            - name: REGISTRY_URI
              value: "<variable>" 
            - name: IMPORT_INTERVAL_MINUTES
              value: "<variable>" """
	      
	      sh "kubectl apply -f tiocsscanner-deployment.yaml"

    	}
    }
    
    stage ('DAST')
    {
    	sh """export SERVICE_IP=$(kubectl get svc --namespace default micro -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"""
	sh """echo http://$SERVICE_IP:80"""
	sh """docker run -t owasp/zap2docker-stable zap-baseline.py -t http://$SERVICE_IP:80/app/employee"""
    }
	
}

		
