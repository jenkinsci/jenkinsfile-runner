CasC Demo
=========
This directory contains resources demonstrating the use of
[Configuration-as-Code](https://github.com/jenkinsci/configuration-as-code-plugin)
with jenkinsfile-runner.

The provided `Jenkinsfile` reads an environment variable that is defined via
JCasC in `config/jenkins.yaml`. The `config` directory is mounted in the
Docker image to `/usr/share/jenkins/ref/casc`, where it is recognized by
Jenkins. Note that this does not need to be a single file, but can be
split up into multiple YAMLs.

Running the demo
----------------
The following commands need to be run in the directory containing this file.

If you have not built the Docker image yet:
(Refer to the [Packaging to Docker image](../../DOCKER.md) page for details on the
Docker packaging process)
````bash
docker build -t jenkinsfile-runner:my-production-jenkins ../..
````

Now that the image is built, run it, mounting the `config` directory:
````bash
docker run --rm -v $PWD:/workspace -v $PWD/config:/usr/share/jenkins/ref/casc jenkinsfile-runner:my-production-jenkins
````

This will print the variable that was set via JCasC.
````
...
[Pipeline] echo
An environment variable configured via JCasC: a value configured via JCasC
...
````
