def call(String s) {
       def Sonarscanner = tool 'Sonarscanner';
    withSonarQubeEnv('Sonarqube') 
    {
           sh "echo ${s}" 
        sh """echo ${Sonarscanner}"""
     sh  """${Sonarscanner}/sonar-scanner -Dsonar.host.url=${s} -Dsonar.login=admin -Dsonar.password=soumianisoumya@123"""   
    }
    
}
