/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

// TODO: Move it to Jenkins Pipeline Library

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
                stage('Checkout') {
                    checkout scm
                }

                stage('Build') {
                    withEnv([
                        "JAVA_HOME=${tool 'jdk8'}",
                        "PATH+MVN=${tool 'mvn'}/bin",
                        'PATH+JDK=$JAVA_HOME/bin',
                    ]) {
                        timeout(30) {
                            String command = 'mvn --batch-mode clean install -Dmaven.test.failure.ignore=true'
                            if (isUnix()) {
                                sh command
                            }
                            else {
                                bat command
                            }
                        }
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

/* Execute our platforms in parallel */
parallel(branches)
