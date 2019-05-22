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
    
    stage ('Check-secrets')
    {
    	sh """
	rm trufflehog || true
	docker run gesellix/trufflehog --json ${props['deploy.gitURL']} > trufflehog
	cat trufflehog	
	"""
    }
    
    stage ('Source Composition Analysis') 
    {
         sh 'rm owasp* || true'
         sh 'wget "https://raw.githubusercontent.com/Devops-Accelerators/Micro/master/owasp-dependency-check.sh" '
         sh 'chmod +x owasp-dependency-check.sh'
         sh 'bash owasp-dependency-check.sh'
         sh 'cat /var/lib/jenkins/OWASP-Dependency-Check/reports/dependency-check-report.xml'
    }
    
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
	withKubeConfig(credentialsId: 'kubernetes-creds', serverUrl: 'https://35.202.204.20') {

		sh """ helm delete --purge ${props['deploy.microservice']} | true"""
		helmdeploy "${props['deploy.microservice']}"
	}
	
    }
	
}
		
