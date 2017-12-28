pipeline {
  agent any
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
           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release info -s "${SID}" -u Portree-Kid -r terramaster'
           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release release -s "${SID}" -u Portree-Kid -r terramaster'
        }
        archiveArtifacts '*terramaster*.jar'
      }
    }
  }
}
