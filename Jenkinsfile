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
                script{
                  def props = readProperties file: 'src/main/resources/build_info.properties'
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
          withMaven(maven: 'Maven 3.5.3') {
            bat "mvn clean install"
          }                   
        }  
        archiveArtifacts '*terramaster*.jar'    
      }
    }
    
    stage( 'deploy' ) {
      steps{
        script{
            if (env.BRANCH_NAME == 'master') {
                def props = readProperties file: 'src/main/resources/build_info.properties'
                def message = props['build.major.number'] + "." + props['build.minor.number'] 
                  //Pipe through tee to get rid of errorlevel
                  withEnv(["SID=${env.sid}"]) {
                    result = bat(returnStdout:true,  script: "C:\\Users\\keith.paterson\\go\\bin\\github-release info -s %SID% -u Portree-Kid -r terramaster -t ${message} 2>&1 | tee").trim()
                  }
                  if( result.trim().indexOf("could not find the release corresponding") < 0 ){
                    withEnv(["SID=${env.sid}"]) {
                      bat "C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t ${message}"
                    }
                  }
                }            
                withEnv(["SID=${env.sid}"]) {
                   bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t ${message} -n terramaster.jar -f ${files}"""
                }
                archiveArtifacts '*terramaster*.jar'
            }
          }              
     }
  }
 post {
    always {
        junit 'target/**/*.xml'
    }
 }
}
