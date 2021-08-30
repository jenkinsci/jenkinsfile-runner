Pipeline Library demo
=====================

This demo shows execution of a simple Declarative Pipeline,
powered by the Jenkinsfile Runner vanilla package.

## Running in Docker

```bash
docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile jenkins4eval/jenkinsfile-runner:1.0-beta-11
```

## Running (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
../../app/target/appassembler/bin/jenkinsfile-runner -p ../../vanilla-package/target/plugins/ -w ../../vanilla-package/target/war/ -f .
```
