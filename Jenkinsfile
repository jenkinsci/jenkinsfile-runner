/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

// TODO: Move it to Jenkins Pipeline Library

def branchName = currentBuild.projectName
def buildNumber = currentBuild.number

/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
List platforms = ['linux']
Map branches = [:]

for (int i = 0; i < platforms.size(); ++i) {
    String label = platforms[i]
    branches[label] = {
        node(label) {
            timestamps {
                ws("platform_${label}_${branchName}_${buildNumber}") {
                    stage('Checkout') {
                        checkout scm
                    }

                    stage('Build') {
                        timeout(60) {
                            infra.runMaven(['clean', 'package', '-Dmaven.test.failure.ignore=true', '-Denvironment=test'])
                        }
                    }

                    // TODO: Add some tests first
                    stage('Archive') {
                        /* Archive the test results */
                        // junit '**/target/surefire-reports/TEST-*.xml'

                        //if (label == 'linux') {
                        //  archiveArtifacts artifacts: '**/target/**/*.jar'
                        //  findbugs pattern: '**/target/findbugsXml.xml'
                        //}
                    }
                }
            }
        }
    }
}

/* Execute our platforms in parallel */
parallel(branches)

stage('Verify demos')
Map demos = [:]
demos['cwp'] = {
    node('docker') {
        timestamps {
            ws("cwp_${branchName}_${buildNumber}") {
                checkout scm
                stage('CWP') {
                    dir('demo/cwp') {
                        sh "make clean buildInDocker run"
                    }
                }
            }
        }
    }
}
demos['databound'] = {
    node('docker') {
        timestamps {
            ws("databound_${branchName}_${buildNumber}") {
                checkout scm
                stage('Databound') {
                    dir('demo/databound') {
                        sh "make clean buildInDocker run"
                    }
                }
            }
        }
    }
}

parallel(demos)

node('docker') {
    ws("container_${branchName}_${buildNumber}") {
        infra.withDockerCredentials {
            def image
            def imageName = "${env.DOCKERHUB_ORGANISATION}/jenkinsfile-runner"
            def imageTag

            stage('Build container') {
                timestamps {
                    def scmVars = checkout scm

                    def shortCommit = scmVars.GIT_COMMIT
                    imageTag = branchName.equals("master") ? "latest" : branchName
                    echo "Creating the container ${imageName}:${imageTag}"
                    image = docker.build("${imageName}:${imageTag}", '--no-cache --rm .')
                }
            }

  // TODO(oleg-nenashev): Reenable once CI is stable
  //          if (branchName.startsWith('master')) {
  //              stage('Publish container') {
  //                  timestamps {
  //                      image.push();
  //                  }
  //              }
  //          }
        }
    }
}

