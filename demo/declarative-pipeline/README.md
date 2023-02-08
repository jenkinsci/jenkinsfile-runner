Pipeline Library demo
=====================

This demo shows execution of a simple Declarative Pipeline,
powered by the Jenkinsfile Runner vanilla package.

## Running in Docker

```bash
docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile ghcr.io/jenkinsci/jenkinsfile-runner:latest
```

## Running (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
../../app/target/appassembler/bin/jenkinsfile-runner -p ../../vanilla-package/target/plugins/ -w ../../vanilla-package/target/war/ -f .
```
