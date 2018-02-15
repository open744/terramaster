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
           def files = findFiles(glob: '**/*terramaster*.jar')
           echo """${files[0].name} ${files[0].path} ${files[0].directory} ${files[0].length} ${files[0].lastModified}"""
           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release info -s %SID% -u Portree-Kid -r terramaster'
           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t 1.9'
           bat 'C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t 1.9 -f '
        }
        archiveArtifacts '*terramaster*.jar'
      }
    }
  }
}
