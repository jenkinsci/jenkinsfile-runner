# Packaging to Docker image
You can package Jenkinsfile Runner with a specific Jenkins image and turn that into a Docker image.
This way, you can ensure people are running Jenkinsfile in the specific Jenkins environment, for example one that's identical to your production environment.

## Building
Edit the `plugins.txt` to to include any plugins you'd like to install. See [the Jenkins Docker README](https://github.com/jenkinsci/docker#preinstalling-plugins) for more information.

Then, build the Jenkinsfile Runner image like this:

```
docker build -t jenkinsfile-runner:my-production-jenkins --build-arg JENKINS_VERSION=2.108 .
```

The optional `JENKINS_VERSION` specifies the version of Jenkins core.

## Usage
Run the image by mounting the directory that contains `Jenkinsfile` into `/workspace`. For example,

```
docker run -v~/foo:/workspace jenkinsfile-runner:my-production-jenkins
```

