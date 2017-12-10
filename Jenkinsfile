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
    //github-release info -s 476fa6b2-c964-4f69-acc3-4ab254eb53d9 -u Portree-Kid
    stage( 'deploy' ) {
      steps{
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        git credentialsId: 'github', url: 'https://github.com/Portree-Kid/terramaster.git'
        bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release info -s ${env.sid} -u Portree-Kid -r terramaster'
        bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release info -s ${env.sid} -u Portree-Kid -r terramaster'
        archiveArtifacts '*terramaster*.jar'
      }
    }
    
    github-release info -s bb120c7e2e7ff9209d5f4be726f2b97f5cb7a541 -u Portree-Kid -r terramaster
  }
}
