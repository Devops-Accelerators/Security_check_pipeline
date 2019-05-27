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
    
    stage ('Scan-image')
    {
    	
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
   /* stage ('deploy to cluster')
    {
    	//helmdeploy "${props['deploy.microservice']}"
	withKubeConfig(credentialsId: 'kubernetes-creds', serverUrl: 'https://35.232.13.35') {

		sh """ helm delete --purge ${props['deploy.microservice']} | true"""
		helmdeploy "${props['deploy.microservice']}"
	}
	
    } */
    
    stage ('DAST')
    {
    	//sh """export SERVICE_IP=$(kubectl get svc --namespace default micro -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"""
	//sh """echo http://$SERVICE_IP:80"""
	//sh """docker run -t owasp/zap2docker-stable zap-baseline.py -t http://$SERVICE_IP:80/app/employee"""
	
	sh """
		docker pull $docImg:$BUILD_NUMBER
		docker run -d --name zapTest -p ${props['deploy.port']}:8080 $docImg:$BUILD_NUMBER
		export ARCHERY_HOST=http://ec2-99-81-179-32.eu-west-1.compute.amazonaws.com:8000
		bash /var/lib/jenkins/archery/zapscan.sh
	"""
    } 
	
}

		
