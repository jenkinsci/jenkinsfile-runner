# Demo: Git and Maven

> **WARNING:** This demo is outdated and needs to be renewed to the new version

Demonstrates a simple Pipeline which checks out from the Git repository and then runs an Apache Maven build.

## Run with Docker

```bash
docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile jenkins4eval/jenkinsfile-runner:maven
```

This example is using Jenkinsfile Runner base image for building projects with Java and Apache Maven.

Image is available on Docker Hub: https://hub.docker.com/r/jenkins4eval/jenkinsfile-runner/tags

Source code for the image is on GitHub: https://github.com/jenkinsci/jenkinsfile-runner-image-packs/tree/main/maven
