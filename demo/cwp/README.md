# Jenkinsfile Runner demo

This demo demonstrates building of Jenkinsfile Runner Docker images
with [Custom WAR Packager](https://github.com/jenkinsci/custom-war-packager/).
This demo bundles some plugins so that it demonstrates a Pipeline execution.

To build the Docker image, run `make clean build`

You can experiment with other `Jenkinsfile`s if needed.
Once the Docker image is built, the demo Jenkinsfile Runner can be started simply as...

    docker run --rm -v $PWD/Jenkinsfile:/workspace/Jenkinsfile jenkins-experimental/jenkinsfile-runner-demo

See a more complex demo [here](https://github.com/jenkinsci/custom-war-packager/blob/master/demo/jenkinsfile-runner/)

## Recording

![Demo recording](recording.gif)

This demo is captured from CLI using [Terminalizer](https://terminalizer.com/).
Source files in YAML are also available in the demo folder.
