# Jenkins Templating Engine demo

This demo shows how to use the [Jenkins Templating Engine](https://plugins.jenkins.io/ptemplating-engine/) (JTE) with Jenkinsfile Runner.

Support for Pipeline as YAML plugin is available starting from 
> insert jenkinsfile-runner release or JTE release here

In this demo we will execute a simple "Hello, world!" Pipeline defined via JTE.

This demo uses JCasC to configure the `libraries` directory as a library source available to the pipeline. 

## Running in Docker

```bash
docker run --rm \
  -v $(pwd)/demo/jenkins-templating-engine:/workspace \
  -v $(pwd):/tmp/libraries \
  -v $(pwd)/demo/jenkins-templating-engine/config:/usr/share/jenkins/ref/casc \
   jenkins4eval/jenkinsfile-runner:dev \
   -jte -pc /workspace/pipeline_config.groovy
```

## Running (without Docker)

Once Jenkinsfile Runner is built locally, the demo can be launched as...

```bash
CASC_JENKINS_CONFIG=config/jenkins.yaml \
../../app/target/appassembler/bin/jenkinsfile-runner \
  -p ../../vanilla-package/target/plugins/ \
  -w ../../vanilla-package/target/war/jenkins.war \
  -jte \
  -f ./Jenkinsfile \
  -pc ./pipeline_config.groovy
```
