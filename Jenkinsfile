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
                script {
                  def props = readProperties file: 'target/maven-archiver/pom.properties'
                  def message = props['version'] 
                  //Pipe through tee to get rid of errorlevel
                  withEnv(["SID=${env.sid}"]) {
                    result = bat(returnStdout:true,  script: "C:\\Users\\keith.paterson\\go\\bin\\github-release info -s %SID% -u Portree-Kid -r terramaster -t ${message} 2>&1 | tee").trim()
                  }
                  if( props.size() == 0 || result.trim().indexOf("could not find the release corresponding") > 0 ) {
                    withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
                      try {
                          withMaven(maven: 'Maven 3.5.3') {
                                bat "mvn release:prepare -Dresume=false"
                          }
                          def releaseProps = readProperties file: 'release.properties'
                      } catch (error) {
                          withMaven(maven: 'Maven 3.5.3') {
                                bat "mvn release:rollback"
                          }
                          error("Release-Prepare failed")
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
         script {
            if (env.BRANCH_NAME != 'master') {
                withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
                  withMaven(maven: 'Maven 3.5.3') {
                    bat "mvn clean install"
                  }                   
                }  
                archiveArtifacts '*terramaster*.jar'    
             }              
           }
             
       }
    }
    
    stage( 'deploy' ) {
      steps{
        script{
            echo env.BRANCH_NAME
            if (env.BRANCH_NAME == 'master') {
                def props = readProperties file: 'target/maven-archiver/pom.properties'
                def releaseProps = readProperties file: 'release.properties'
                def version = props['version'] 
                def tag = releaseProps[ 'scm.tag' ]
                echo "Releasing ${version} Tag : ${tag}"

                withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
                  withMaven(maven: 'Maven 3.5.3') {
                    bat "mvn release:perform"
                  }                   
                }  
                  //Pipe through tee to get rid of errorlevel
                withEnv(["SID=${env.sid}"]) {
                    result = bat(returnStdout:true,  script: "C:\\Users\\keith.paterson\\go\\bin\\github-release info -s %SID% -u Portree-Kid -r terramaster -t ${tag} 2>&1 | tee").trim()
                    echo "${result} " + result.trim().indexOf("could not find the release corresponding")
                    if( result.trim().indexOf("could not find the release corresponding") > 0 ) {
                      bat "C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t ${tag}"
                    }                            
                    bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t ${tag} -l ${version} -n ${tag}.jar -f target/${tag}.jar"""
                }
            }
            archiveArtifacts '*terramaster*.jar'
          }
        }              
     }
  }
  post {
    always {
        junit 'target/surefire-reports/*.xml'
    }
  }
}
