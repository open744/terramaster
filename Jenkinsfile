pipeline {
  agent any
  environment{
      def files = findFiles(glob: '**/terramaster.jar')[0]
  }

  stages {
    stage( 'build' ) {
      steps{
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        archiveArtifacts '*terramaster*.jar'    
      }
    }
    
    stage( 'deploy' ) {
      steps{
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        git credentialsId: 'github', url: 'https://github.com/Portree-Kid/terramaster.git'
        withEnv(["SID=${env.sid}"]) {
           echo """${files}"""
//           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t 1.10'
           bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t 1.10 -f ${files}"""
        }
        archiveArtifacts '*terramaster*.jar'
      }
    }
  }
}
