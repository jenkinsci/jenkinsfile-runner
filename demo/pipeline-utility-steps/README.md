# Jenkinsfile Runner demo

This demo demonstrates how to use the pipeline-utility-steps

The Jenkinsfile Runner can be started simply as...

```bash
docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile \
  ghcr.io/jenkinsci/jenkinsfile-runner:latest 
```
