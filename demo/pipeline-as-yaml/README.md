# Pipeline as YAML demo

> **WARNING:** This demo is outdated and needs to be renewed to the new version

This demo shows how to use [Pipeline as YAML](https://plugins.jenkins.io/pipeline-as-yaml/) in Jenkinsfile Runner.

In this demo we will execute a simple "Hello, world!" Pipeline defined as YAML.

## Running in Docker

```bash
docker run --rm \
    -v $(pwd)/Jenkinsfile.yml:/workspace/Jenkinsfile.yml \
    ghcr.io/jenkinsci/jenkinsfile-runner:latest 
```
