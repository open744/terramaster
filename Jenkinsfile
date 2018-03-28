pipeline {
  agent any
  environment{
      def files = findFiles(glob: '**/terramaster.jar')[0].getPath()
  }
  stages {
    stage('version') {
        steps{
          script{
                     if (env.BRANCH_NAME == 'master') {
            bat 'git config --global credential.helper cache'
            git credentialsId: 'github', url: "${env.GIT_URL}", branch: "${env.GIT_BRANCH}"
            script{
              def props = readProperties file: 'resources/build_info.properties'
              def message = props['build.major.number'] + "." + props['build.minor.number'] 
              //Pipe through tee to get rid of errorlevel
              withEnv(["SID=${env.sid}"]) {
                result = bat(returnStdout:true,  script: "C:\\Users\\keith.paterson\\go\\bin\\github-release info -s %SID% -u Portree-Kid -r terramaster -t ${message} 2>&1 | tee").trim()
              }
              if( result.trim().indexOf("could not find the release corresponding") < 0 ){
                    withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
                      withAnt('installation' : 'apache-ant-1.10.1') {
                        bat "ant minor"
                      }
                    }  
                 }
            }            
        }
              
          }
      }
    }

    stage( 'build' ) {
      steps{
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME']])
        {
            bat "git add resources/build_info.properties"
            script{
              def props = readProperties file: 'resources/build_info.properties'
              def message = props['build.major.number'] + "." + props['build.minor.number'] + "_" + props['build.number']
              bat "git commit -m \"Build ${message} \""
              bat "git push ${env.GIT_URL}"
            }
          //bat "git  -c core.askpass=true  push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/Portree-Kid/terramaster.git#${env.GIT_BRANCH}"
        }
        archiveArtifacts '*terramaster*.jar'    
      }
    }
    
    stage( 'deploy' ) {
      steps{
        script{

            if (env.BRANCH_NAME == 'master') {
                def props = readProperties file: 'resources/build_info.properties'
                def message = props['build.major.number'] + "." + props['build.minor.number'] 
                withEnv(["SID=${env.sid}"]) {
                   bat "C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t ${message}"
                   bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t ${message} -n terramaster.jar -f ${files}"""
                }
                archiveArtifacts '*terramaster*.jar'
            }
          }
        }
     }
  }
}
