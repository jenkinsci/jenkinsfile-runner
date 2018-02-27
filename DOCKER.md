# Packaging to Docker image
You can package Jenkinsfile Runner with a specific Jenkins image and turn that into a Docker image.
This way, you can ensure people are running Jenkinsfile in the specific Jenkins environment, for example one that's identical to your production environment.

## Building
First, follow [the preparation step in main README](README.md#preparation) and create a directory full of plugins. Name it `plugins` and put it next to `Dockerfile`.

Then build the Jenkinsfile Runner image like this:

```
docker build -t jenkinsfile-runner:my-production-jenkins --build-arg JENKINS_VERSION=2.108 .
```

The optional `JENKINS_VERSION` specifies the version of Jenkins core.

## Usage
Run the image by mounting the directory that contains `Jenkinsfile` into `/workspace`. For example,

```
docker run -v~/foo:/workspace jenkinsfile-runner:my-production-jenkins
```

