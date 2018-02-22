pipeline {
  agent any
  environment{
      def files = findFiles(glob: '**/terramaster.jar')[0].getPath()
  }
  stages {
    stage( 'build' ) {
      steps{
        bat 'git config --global credential.helper cache'
        git credentialsId: 'github', url: "${env.GIT_URL}", branch: "${env.GIT_BRANCH}"
        script{
            if (env.BRANCH_NAME == 'master') {
              withAnt('installation' : 'apache-ant-1.10.1') {
                //bat "ant minor"
              }
            }
        }
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        script{
          def props = readProperties file: 'resources/build_info.properties'
        }
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME']])
        {
          bat 'git status'  
          bat "git add resources/build_info.properties"
            script{
              def props = readProperties file: 'resources/build_info.properties'
              def message = props['build.major.number'] + "." + props['build.minor.number'] 
              bat "git commit -m \"Version ${message}\""
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
              withEnv(["SID=${env.sid}"]) {
                  def props = readProperties file: 'build_info.properties'
                  def message = props['build.major.number'] + "." + props['build.minor.number'] 
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
