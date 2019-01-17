Jenkinsfile Runner demo
===

This demo demonstrates building of Jenkinsfile Runner Docker images
with [Custom WAR Packager](https://github.com/jenkinsci/custom-war-packager/)
using a JDK 11 based Docker image.
This demo bundles some plugins so that it demonstrates a Pipeline execution.

To build the Docker image, run `make clean build`

You can experiment with other `Jenkinsfile`s if needed.
Once the Docker image is built, the demo Jenkinsfile Runner can be started simply as...

    docker run --rm -v $PWD/Jenkinsfile:/workspace/Jenkinsfile jenkins-experimental/jenkinsfile-runner-demo

See a more complex demo [here](https://github.com/jenkinsci/custom-war-packager/blob/master/demo/jenkinsfile-runner/)
