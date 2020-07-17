Pipeline as YAML demo
=====================

This demo shows how to use [Pipeline as YAML](https://plugins.jenkins.io/pipeline-as-yaml/) in Jenkinsfile Runner.
It executes a simple "Hello, world!" Pipeline defined as YAML

## Running (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
java -jar ../../app/target/jenkinsfile-runner-standalone.jar -p ../../vanilla-package/target/plugins/ -f . 
```

The demo will likely fail, but you will be able to see that the library is loaded implicitly.
