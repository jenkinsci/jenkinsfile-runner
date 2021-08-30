Pipeline Library demo
=====================

This demo shows how to declare a Pipeline library with JCasC and to use it in the Pipeline.
Later this demo will be reworked to the native bundling.

## Running (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
CASC_JENKINS_CONFIG=$(pwd)/jenkins.yaml ../../app/target/appassembler/bin/jenkinsfile-runner -p ../../vanilla-package/target/plugins/ -f . 
```

The demo will likely fail, but you will be able to see that the library is loaded implicitly.
