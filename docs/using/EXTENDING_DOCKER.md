# Extending Jenkinsfile Runner Docker images

Jenkinsfile Runner provides a vanilla image which includes some plugins and configurations.
Usually, it is not enough to run a real Jenkins Pipelines.
Users are expected to extend the base Jenkinsfile Runner images to customize them for their pipelines.
Common use-cases:

* Installing plugins
* Adding tools
* Pre-configuring Jenkinsfile Runner so all plugins and features are configured to work in user environment
  when execution starts up.

## Installing plugins

Say you want to install a specific plugin (e.g. Slack Plugin to send notifications to a Slack channel ).
Jenkinsfile Runner Vanilla image includes [Plugin Installation Manager Tool](https://github.com/jenkinsci/plugin-installation-manager-tool).
It can be used to customize plugins when building your image.

You can just create two files with the following content:

- plugins.txt
```
slack
```

- Dockerfile
```
FROM jenkins4eval/jenkinsfile-runner
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN cd /app/jenkins && jar -cvf jenkins.war *
RUN java -jar /app/bin/jenkins-plugin-manager.jar --war /app/jenkins/jenkins.war --plugin-file /usr/share/jenkins/ref/plugins.txt && rm /app/jenkins/jenkins.war
```

Now you have a custom Jenkinsfile Runner image with your own plugins installed!
See the [Plugin Installation Manager Tool documentation](https://github.com/jenkinsci/plugin-installation-manager-tool) for more information about available features.

## Adding tools

| NOTE: This documentation section has not been implemented yet. Please feel free to submit a pull request! |
| --- |

## Configuring Jenkins

Jenkinsfile Runner supports [Groovy Hook Scripts](https://www.jenkins.io/doc/book/managing/groovy-hook-scripts/) for managing configurations.

The Vanilla image also includes the [Jenkins Configuration-as-Code plugin](https://github.com/jenkinsci/configuration-as-code-plugin) which can be used to configure the instance.
You can mount JCasC YAML files into the `/usr/share/jenkins/ref/casc` directory as a volume.
Refer to the [Configuration-as-Code documentation](https://github.com/jenkinsci/configuration-as-code-plugin)
for available options and the [JCasC demo](/demo/casc/README.md) for an example.

## Custom War Packager

There is an alternative to Vanilla Dockerfile that gives a huge versatility when it comes to generate the docker images.
See the demo for a better understanding on [how to build Jenkinsfile Runner with Custom WAR Packager](/demo/cwp).

Once the docker image is generated you can execute it in the same way as a Vanilla image with the only exception of the master workspace.
The default directory is `/build` as well but it can be overridden using the `--runworkspace` Jenkinsfile Runner option.

A special case worth mentioning is the joint use of Custom War Packager and [Dependabot](https://dependabot.com) to maintain your docker image up to date.
Custom War Packager offers the capability to generate the Jenkinsfile Runner docker image through a pom file.
Setting the plugins as dependencies and configuring Dependabot to scan that pom.xml will keep the plugins for the image updated to their latest versions.
The [ci.jenkins.io-runner project](https://github.com/jenkinsci/ci.jenkins.io-runner) can be used as reference:

* The ci.jenkins.io-runner project makes use of Custom War Packager to build its own Jenkinsfile Runner image.
See the [Makefile](https://github.com/jenkinsci/ci.jenkins.io-runner/blob/66c959ca68aa3379d8eb2bdae39c884adf1fe908/Makefile#L39-L42).
* The [packager-config.yml](https://github.com/jenkinsci/ci.jenkins.io-runner/blob/eb571f5594708c3fbad167032326765257398354/packager-config.yml#L7-L9) file configures the build settings so that the list of plugins to install in the Jenkinsfile Runner image is gathered from the`dependencies` section in the pom file in the repository.
* Dependabot is configured to scan all the dependencies in pom.xml and Dockerfile files so it will open one pull request per each dependency susceptible to be updated.

This very one repository has Dependabot configured as well and it could be used as another reference.
See [.dependabot](.dependabot) folder for further details.
