# Packaging to Docker image
You can package Jenkinsfile Runner with a specific Jenkins image and turn that into a Docker image.
This way, you can ensure people are running Jenkinsfile in the specific Jenkins environment, for example one that's identical to your production environment.

## Building
Edit the `plugins.txt` to to include any plugins you'd like to install. See [the Jenkins Docker README](https://github.com/jenkinsci/docker#preinstalling-plugins) for more information.

Then, build the Jenkinsfile Runner image like this:

```
docker build -t jenkinsfile-runner:my-production-jenkins --build-arg JENKINS_VERSION=2.121.1 .
```

If you are rebuilding against latest `lts` image, pass `--no-cache` argument to command above
to avoid using stale layers

The optional `JENKINS_VERSION` specifies the version of Jenkins core.

## Usage
Run the image by providing path to `Jenkinsfile` . For example,

```bash
docker run --rm -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins
```

Optionally, if you want to change default parameters for plugins or workspace, you can get onto the container
by overriding entrypoint - binary is placed in `/app/bin/jenkinsfile-runner`

```bash
$ docker run --rm -it -v $PWD/test:/workspace --entrypoint bash jenkinsfile-runner:my-production-jenkins
root@dec4c0f12478:/src# cp -r /app/jenkins /tmp/jenkins
root@dec4c0f12478:/src# /app/bin/jenkinsfile-runner -w /tmp/jenkins -p /usr/share/jenkins/ref/plugins -f /workspace
```
