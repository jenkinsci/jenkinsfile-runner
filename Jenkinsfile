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
        node(label + " && docker") {
            timestamps {
                ws("platform_${label}_${branchName}_${buildNumber}") {
                    stage('Checkout') {
                        checkout scm
                    }

                    stage('Build') {
                        timeout(60) {
                            infra.runMaven(['clean', 'install', '-Dset.changelist', '-Dmaven.test.failure.ignore=true', '-Denvironment=test', '-Ppackage-app,package-vanilla,jacoco,run-its'], '11')
                        }
                    }

                    stage('Archive') {
                        /* Archive the test results */
                        junit '**/target/surefire-reports/TEST-*.xml'

                        if (label == 'linux') {
                            infra.prepareToPublishIncrementals()
                            
                            recordIssues(
                              enabledForFailure: true, aggregatingResults: true, 
                              tools: [java(), spotBugs(pattern: '**/target/spotbugsXml.xml')]
                            )

                            recordCoverage(tools: [[parser: 'JACOCO', pattern: 'vanilla-package/target/site/jacoco-aggregate/*.xml']], sourceCodeRetention: 'MODIFIED')
                        }
                    }
                }
            }
        }
    }
}

/* Execute our platforms in parallel */
parallel(branches)

// TODO: Rework Custom WAR Packager
/*
stage('Verify Custom WAR Packager demo')
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

parallel(demos)*/

// TODO: Run integration tests

infra.maybePublishIncrementals()
