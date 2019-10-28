Demo: Git and Maven
===================

Demonstrates simple Pipeline which checks out from the Git repository and then runs a Maven build.

### Run with Docker

1. Build the Docker image in the repo
2. Run the standard JFR Docker run command from the main README.
   You can add a Maven volume for better performance.

### Run (without Docker)

```shell
java -jar ../../app/target/jenkinsfile-runner-standalone.jar -p ../../vanilla-package/target/plugins/ -f . 
```
