Demo: Git and Maven
===================

Demonstrates simple Pipeline which checks out from the Git repository and then runs a Maven build.

| WARNING: This demo is based on the unreleased Vanilla version. Git support will be added in 1.0-beta-12 |
| --- |

### Run with Docker

```
docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile jenkins4eval/jenkinsfile-runner:latest
```

### Run (without Docker)

```shell
java -jar ../../app/target/jenkinsfile-runner-standalone.jar -p ../../vanilla-package/target/plugins/ -w ../../vanilla-package/target/war/jenkins.war -f . 
```
