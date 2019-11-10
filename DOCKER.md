# Packaging to Docker image
You can package Jenkinsfile Runner with a specific Jenkins image and turn that into a Docker image.
This way, you can ensure people are running Jenkinsfile in the specific Jenkins environment, for example one that's identical to your production environment.

## Building the Vanilla image
This repository offers a Dockerfile to generate a Jenkinsfile Runner docker image.

Edit the `plugins.txt` to include any plugins you'd like to install. See [the Jenkins Docker README](https://github.com/jenkinsci/docker#preinstalling-plugins) for more information.

Then, build the Jenkinsfile Runner image like this:

```bash
docker build -t jenkinsfile-runner:my-production-jenkins --build-arg JENKINS_VERSION=2.121.1 .
```

If you are rebuilding against latest `lts` image, pass `--no-cache` argument to command above
to avoid using stale layers

The optional `JENKINS_VERSION` specifies the version of Jenkins core.

Note that the master workspace is mapped to `/build` by default.
This directory can be exposed as a volume.


## Usage
Run the image by providing path to `Jenkinsfile` . For example,

```bash
docker run --rm -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins
```

In same cases you may be interested in passing extra options.
Use the `JAVA_OPTS` environment variable to do that.

```bash
docker run --rm -e JAVA_OPTS="-Xms 256m" -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins
```

It is also possible to pass arguments to the Jenkinsfile or to execute it outside a sandbox environment with the `-ns` and `-a` options.
Using a non-sandbox environment may pose potential security risks.
we strongly encourage you not to use this mode unless it is strictly necessary and always with extreme care and at your own risk.

```bash
docker run --rm -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins -ns -a "my_param=any_value"
```

Optionally, if you want to change default parameters for plugins or workspace, you can get onto the container
by overriding entrypoint - binary is placed in `/app/bin/jenkinsfile-runner`.

```bash
$ docker run --rm -it -v $PWD/test:/workspace --entrypoint bash jenkinsfile-runner:my-production-jenkins
root@dec4c0f12478:/src# cp -r /app/jenkins /tmp/jenkins
root@dec4c0f12478:/src# /app/bin/jenkinsfile-runner -w /tmp/jenkins -p /usr/share/jenkins/ref/plugins -f /workspace
```

Optionally, if you need special Jenkins configuration, you can mount JCasC YAML files into the
`/usr/share/jenkins/ref/casc` directory as a volume. Refer to the
[Configuration-as-Code documentation](https://github.com/jenkinsci/configuration-as-code-plugin)
for available options and the [JCasC demo](demo/casc/README.md) for an example.

To get an interactive Jenkins CLI shell in the container, pass
`-i -e FORCE_JENKINS_CLI=true` to `docker run` as extra parameters.

## Debug
In case you want to debug Jenkinsfile Runner, you need to use the "Vanilla" Docker image built following the steps mentioned in the section above.

Then, set the `DEBUG` environment variable and expose the port where to connect the remote debug. Note Jenkinsfile Runner itself
considers 5005 as debugging port but you can map such port to whatever value you prefer through Docker port mapping.

```bash
docker run --rm -e DEBUG=true -p 5005:5005 -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins
```

In case you are having issues when the Docker image is generated in another way (for example through [Custom WAR Packager](https://github.com/jenkinsci/custom-war-packager/)),
you can directly pass `JAVA_OPTS` using the Docker run arguments:

```bash
docker run --rm -e JAVA_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005' -p 5005:5005 -v $PWD/test:/workspace jenkinsfile-runner:my-production-jenkins
```

## Custom War Packager
There is an alternative to Vanilla Dockerfile that gives a huge versatility when it comes to generate the docker images.
See the demo for a better understanding on [how to build Jenkinsfile Runner with Custom WAR Packager](demo/cwp).

Once the docker image is generated you can execute it in the same way as a Vanilla image with the only exception of the master workspace.
The default directory is `/build` as well but it can be overridden using the `--runworkspace` Jenkinsfile Runner option.

A special case worth mentioning is the joint use of Custom War Packager and [Dependabot](https://dependabot.com) to maintain your docker image up to date.
Custom War Packager offers the capability to [generate the Jenkinsfile Runner docker image through a pom file](https://github.com/jenkinsci/custom-war-packager/tree/master/demo/artifact-manager-s3-pom).
Setting the plugins as dependencies and configuring Dependabot to scan that pom.xml will keep the plugins for the image updated to their latest versions.
The [ci.jenkins.io-runner project](https://github.com/jenkinsci/ci.jenkins.io-runner) can be used as reference:

* The ci.jenkins.io-runner project makes use of Custom War Packager to build its own Jenkinsfile Runner image.
See the [Makefile](https://github.com/jenkinsci/ci.jenkins.io-runner/blob/66c959ca68aa3379d8eb2bdae39c884adf1fe908/Makefile#L39-L42).
* The [packager-config.yml](https://github.com/jenkinsci/ci.jenkins.io-runner/blob/eb571f5594708c3fbad167032326765257398354/packager-config.yml#L7-L9) file configures the build settings so that the list of plugins to install in the Jenkinsfile Runner image is gathered from the`dependencies` section in the pom file in the repository.
* Dependabot is configured to scan all the dependencies in pom.xml and Dockerfile files so it will open one pull request per each dependency susceptible to be updated.

This very one repository has Dependabot configured as well and it could be used as another reference.
See [.dependabot](.dependabot) folder for further details.
