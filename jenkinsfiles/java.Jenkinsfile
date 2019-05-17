@Library('my_shared_library')_

def workspace;
def branch;
def dockerImage;
def props='';
def microserviceName;
def port;
def docImg;
def gitUrl;
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
    
    stage (Check-secrets){
    	sh """
	rm trufflehog || true
	docker run gesellix/trufflehog --json ${props['gitURL']} > trufflehog
	cat trufflehog	
	"""
    }
    stage ('create war')
    {
    	mavenbuildexec "mvn build"
    }
    
    stage ('Deploy to tomcat'){
    
    }
        
    Stage ('DAST'){
    
    }
	
}
		
