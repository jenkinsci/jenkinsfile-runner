Jenkinsfile Runner Troubleshooting Demo
=====================

This demo shows execution of a simple Declarative Pipeline,
powered by the Jenkinsfile Runner vanilla package.

## Running locally (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
java -Djava.util.logging.config.file=verbose-logging.properties -jar ../../app/target/jenkinsfile-runner-standalone.jar -p ../../vanilla-package/target/plugins/ -w ../../vanilla-package/target/war/jenkins.war -f .
```
## Docker example

To be published after the 1.0-beta-19 release.
Feel free to submit a patch!
