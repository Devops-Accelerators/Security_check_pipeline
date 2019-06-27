properties([parameters([[$class: 'GlobalVariableStringParameterDefinition', defaultValue: '', description: 'name of the microservice/repo name', name: 'MicroserviceName'], [$class: 'GlobalVariableStringParameterDefinition', defaultValue: '', description: 'HTTPs Url of the github repository', name: 'apiRepoURL'], [$class: 'GlobalVariableStringParameterDefinition', defaultValue: '', description: 'port for kubernates', name: 'port'], credentials(credentialType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl', defaultValue: '', description: '', name: 'gitCred', required: false)])])

def branchName;
def gitURL;
def props;
def microserviceName;
def commit_username;
def commit_Email, repoName;
def gituserName;
node { 
	stage ('Checkout Code')
		{
			try{
			checkout scm
        		workspace = pwd ()
			commit_username=sh(returnStdout: true, script: '''username=$(git log -1 --pretty=%an) 
                                                            echo ${username%@*} ''').trim();
			commit_username=sh(returnStdout: true, script: """echo ${commit_username} | sed 's/48236651+//g'""").trim()
			commit_Email=sh(returnStdout: true, script: '''Email=$(git log -1 --pretty=%ae) 
                                                            echo $Email''').trim();
			props = readProperties  file: """seedJob.properties"""
       			microserviceName = sh(returnStdout: true, script: """echo ${MicroserviceName} | sed 's/[\\._-]//g'""").trim()
			microserviceName = microserviceName.toLowerCase()
			sh"""echo ${microserviceName}""" 
			
			repoName=sh(returnStdout: true, script: """echo \$(basename ${apiRepoURL.trim()})""").trim();
			repoName=sh(returnStdout: true, script: """echo ${repoName} | sed 's/.git//g'""").trim()
			sh"""echo ${repoName}"""
			gituserName=sh(returnStdout: true, script: """echo \$(dirname ${apiRepoURL.trim()})""").trim();
			gituserName=sh(returnStdout: true, script: """echo \$(basename ${gituserName.trim()})""").trim();
			sh"""echo ${gituserName}"""
			}
			catch (error) {
				currentBuild.result='FAILURE'
				notifyBuild(currentBuild.result, "At Stage Checkout Code", commit_Email, "")
				echo """${error.getMessage()}"""
				throw error
			}
		}
    
 stage ('Create CI Pipeline')
	{	try{			
			createpipelinejob(microserviceName.trim(), apiRepoURL.trim())
			}
	 		catch (error) {
				currentBuild.result='FAILURE'
				notifyBuild(currentBuild.result, "At Stage Create CI Pipeline", commit_Email, "")
				echo """${error.getMessage()}"""
				throw error
			}
		}
	
  stage ('Add Repo Webhook')
		{
			
			withCredentials([string(credentialsId:"${props['github.token']}", variable: 'githubCredentials'),
			usernameColonPassword(credentialsId: 'jenkinsadminCredentials', variable: 'jenkinsAdminCredentials')]) 
			{
				try {
	
				createGithubWebhook(repoName.trim(), props['jenkins.server'], props['gitApi.server'],"""${gituserName}""",githubCredentials)
			       	}
				catch (error) {
				currentBuild.result='FAILURE'
				notifyBuild(currentBuild.result, "At Stage Add Repo Webhook", commit_Email, "")
				echo """${error.getMessage()}"""
				throw error
				}
			}
		}

  stage ('Add pipeline Scripts to Repository')
		{
			try{
			withCredentials([string(credentialsId:"${props['github.password']}", variable: 'git'),
			usernameColonPassword(credentialsId: 'jenkinsadminCredentials', variable: 'jenkinsAdminCredentials')]) 
					{
					
					sh """rm -rf ${repoName.trim()} 
					rm -rf ${microserviceName.trim()}
					git clone ${apiRepoURL}""".trim()
					echo "Cloning is done here"
					//add app name and definition file name			
					sh """
					rm -f ${repoName}/Jenkinsfile
					#rm -rf ${microserviceName.trim()}
					rm -rf helmchart
					rm -f ${repoName.trim()}/deploy.properties
					rm -f ${repoName}/Dockerfile
					rm -f ${repoName.trim()}/sonar-project.properties
					echo "second step is done"
					
					cd ${repoName.trim()}
					#Create deploy.properties file
					cat >> deploy.properties << EOF
deploy.microservice=${microserviceName.trim()}
deploy.port=${port.trim()}
deploy.sonarqubeserver=${props['sonar.server']}
deploy.dockerhub=${props['dockerhub.name']}
deploy.gitURL=${apiRepoURL.trim()}
deploy.archery=${props['archery.server']}"""
					sh """
					cd ${repoName.trim()}
					#Create sonar.properties file
					cat >> sonar-project.properties << EOF
sonar.projectKey=${microserviceName.trim()}
sonar.projectName=${microserviceName.trim()}
sonar.projectVersion=1.0			
sonar.language=java
sonar.sources=src/main
sonar.sourceEncoding=UTF-8
sonar.java.binaries=target/classes
sonar.test.exclusions=src/test/java/com/mindtree/BasicApp"""

					sh """ cd ${repoName.trim()}																
					cp -f ../jenkinsfiles/java.Jenkinsfile Jenkinsfile
					rm -rf helmchart
					cp -f ../Dockerfile Dockerfile
					
					echo "copy helm basic template"
					sed -i "s/microservicename/${microserviceName}/g" ../java-micro/Chart.yaml
					cp -rf ../java-micro helmchart
					
					git config --global user.name ${commit_username}
					git config --global user.email ${commit_Email}
					git init
					git add .
					git pull
					git commit -m "pipeline Script added by seed job" | true
					git remote rm origin
					git remote add origin https://${commit_username}:${git}@github.com/${gituserName}/${repoName.trim()}.git
					git remote -v
					git push -f origin master 
					cd ..
					rm -rf ${repoName.trim()}"""	
			}
			}
			catch (error) {
				currentBuild.result='FAILURE'
				notifyBuild(currentBuild.result, "At Stage Add pipeline Scripts to Repository", commit_Email, "")
				echo """${error.getMessage()}"""
				throw error
			}
		}
	stage ('Execute seed job'){
		try{
		build job: "${microserviceName}", propagate: false
		}
		catch (error) {
				currentBuild.result='FAILURE'
				notifyBuild(currentBuild.result, "At Stage Execute seed job", commit_Email, "")
				echo """${error.getMessage()}"""
				throw error
			}
	}
	notifyBuild(currentBuild.result, "", commit_Email, """Version tag created with name branch \n Build successful. """)
}

def notifyBuild(String buildStatus, String buildFailedAt, String commit_Email, String bodyDetails) 
{
	buildStatus = buildStatus ?: 'SUCCESS'
	def details = """Please find attahcment for log and Check console output at ${BUILD_URL}\n \n "${bodyDetails}"
		\n"""
	emailext attachLog: true,
	notifyEveryUnstableBuild: true,
	recipientProviders: [[$class: 'RequesterRecipientProvider']],
	body: details, 
	subject: """${buildStatus}: Job ${microserviceName} [${BUILD_NUMBER}] ${buildFailedAt}""", 
	to: """${commit_Email}"""
	
	slackSend message: "Build Result: ${buildStatus} \n ${BUILD_URL} "
}

def createpipelinejob(String jobName, String gitURL)
{
    jobDsl failOnMissingPlugin: true, 
	    sandbox: true,
           scriptText: """pipelineJob("${jobName}") {
	   triggers {
                        githubPush()
                    }
            definition {
                        cpsScm {
                        	scm {
                        		git {
                        			remote {
                        				name('remoteB')
                       					url('${gitURL}')
							credentials('${gitCred}')
                        				}
                        			branch("*/master")
                        			extensions {}	
                        			}	
                        		}
                        		scriptPath("Jenkinsfile")	
                       		}
                        }
                       }"""
}

def createGithubWebhook(String repoName, String jenkinsServer, String githubApiURL,String owner, String credentials)
{
	sh """curl -v -H "Content-Type:application/json" POST -d \'{ "name": "web", "active": true, "events": ["push"], "config": {"url": "${jenkinsServer}github-webhook/", "content_type": "json"}}\' \\
	${githubApiURL}/repos/${owner}/${repoName}/hooks?access_token=${credentials}"""
}
