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
  }
}
