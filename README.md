# Jenkinsfile Runner

[![GitHub tag (latest SemVer pre-release)](https://img.shields.io/github/tag-pre/jenkinsci/jenkinsfile-runner?label=changelog)](https://github.com/jenkinsci/jenkinsfile-runner/releases/latest)
[![Docker Pulls](https://img.shields.io/docker/pulls/jenkins4eval/jenkinsfile-runner?label=docker%20pulls%20%28vanilla%29)](https://hub.docker.com/r/jenkins4eval/jenkinsfile-runner)
[![GitHub contributors](https://img.shields.io/github/contributors/jenkinsci/jenkinsfile-runner)](https://github.com/jenkinsci/jenkinsfile-runner/graphs/contributors)

Jenkinsfile Runner is an experiment to package Jenkins pipeline execution as a command line tool.
The intend use cases include:

* Use Jenkins in Function-as-a-Service context
* Assist editing `Jenkinsfile` locally
* Integration test shared libraries

This repository includes the Jenkinsfile Runner sources and also the base (aka "vanilla") Docker image.
This Docker image includes the minimum required set of plugins for running pipelines, but it needs to be extended in order to run real-world pipelines.
See the documentation below for more information.

## Quick Demo

The demo below demonstrates running  of a simple Pipeline with Jenkinsfile Runner:

![Jenkinsfile Runner Demo](./demo/cwp/recording.gif)

See [this directory](./demo/cwp/) for the source codes of the demo.
There are more demos available in the project.

## Usage in command-line
Jenkinsfile Runner can be run in the command line or in Docker.
In case you want to run it in the command line just follow these steps:

1. Download the jar file available in [artifactory](https://repo.jenkins-ci.org/webapp/#/home) or build the source code from this repository (see [contributing guidelines](./CONTRIBUTING.md))
2. Prepare the execution environment
3. Run the command

### Preparation
Find `jenkins.war` that represents the version of Jenkins that you'd like to use,
then unzip it somewhere.
```
wget http://mirrors.jenkins.io/war-stable/latest/jenkins.war
unzip jenkins.war -d /tmp/jenkins
```

Next, create a directory and assemble all the plugins that you'd like to use with the build.
One way to do this is to run the Jenkins setup wizard and install the default set of plugins.
This is a gap intended to be filled with [configuration as code](https://github.com/jenkinsci/configuration-as-code-plugin)
```
JENKINS_HOME=/tmp/jenkins_home java -jar jenkins.war
# go to http://localhost:8080/, follow the installation step
# and install the recommended set of plugins
```

### Execution
Say you have your Git repository checked out at `~/foo` that contains `Jenkinsfile` and your source code.
You can now run Jenkinsfile Runner like this:

```
jenkinsfile-runner -w <path to war> -p <path to plugins> -f <path to Jenkinsfile> [-a "param1=Hello" -a "param2=value2"]
```

Sample Jenkinsfile:

```groovy
$ cat ~/foo/Jenkinsfile
pipeline {
    agent any
    parameters {
        string(name: 'param1', defaultValue: '', description: 'Greeting message')
        string(name: 'param2', defaultValue: '', description: '2nd parameter')
    }
    stages {
        stage('Build') {
            steps {
                echo 'Hello world!'
                echo "message: ${params.param1}"
                echo "param2: ${params.param2}"
                sh 'ls -la'
            }
        }
    }
}
```

Output:

```
$ ./app/target/appassembler/bin/jenkinsfile-runner -w /tmp/jenkins -p /tmp/jenkins_home/plugins -f ~/foo/ -a "param1=Hello&param2=value2"
Started
Running in Durability level: PERFORMANCE_OPTIMIZED
Running on Jenkins in /tmp/jenkinsTests.tmp/jenkins8090792616816810094test/workspace/job
[Pipeline] node
[Pipeline] {
[Pipeline] // stage
[Pipeline] stage
[Pipeline] { (Build)
[Pipeline] echo
Hello world!
[Pipeline] echo
message: Hello
[Pipeline] echo
param2: value2
[Pipeline] sh
[job] Running shell script
+ ls -la
total 12
drwxrwxr-x 2 kohsuke kohsuke 4096 Feb 24 15:36 .
drwxrwxr-x 4 kohsuke kohsuke 4096 Feb 24 15:36 ..
-rw-rw-r-- 1 kohsuke kohsuke    0 Feb 24 15:36 abc
-rw-rw-r-- 1 kohsuke kohsuke  179 Feb 24 15:36 Jenkinsfile
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
Finished: SUCCESS
```

The exit code reflects the result of the build. The `test` directory of this workspace includes a very simple
example of Jenkinsfile that can be used to demo Jenkinsfile Runner.

### CLI options
The executable of Jenkinsfile Runner allows its invocation with these cli options:

```
 # Usage: jenkinsfile-runner -w [warPath] -p [pluginsDirPath] -f [jenkinsfilePath] [other options]
 --runHome FILE              : Path to the empty Jenkins Home directory to use for
                               this run. If not specified a temporary directory
                               will be created. Note that the folder specified via
                               --runHome will not be disposed after the run.
 --runWorkspace FILE         : Path to the workspace of the run to be used within
                               the node{} context. It applies to both Jenkins
                               master and agents (or side containers) if any.
                               Requires Jenkins 2.119 or above
 -a (--arg)                  : Parameters to be passed to workflow job. Use
                               multiple -a switches for multiple params
 --cli                       : Launch interactive CLI. (default: false)
 -u (--keep-undefined-parameters) : Keep undefined parameters if set, defaults
                                    to false.
-f (--file) FILE            : Path to Jenkinsfile (or directory containing a
                               Jenkinsfile) to run, default to ./Jenkinsfile.
 -ns (--no-sandbox)          : Disable workflow job execution within sandbox
                               environment
 -p (--plugins) FILE         : plugins required to run pipeline. Either a
                               plugins.txt file or a /plugins installation
                               directory. Defaults to plugins.txt.
 -n (--job-name) VAL         : Name of the job the run belongs to, defaults to 'job'
 -b (--build-number) N       : Build number of the run, defaults to 1.
 -c (--cause) VAL            : A string describing the cause of the run.
                               It will be attached to the build so that it appears in the
                               build log and becomes available to plug-ins and pipeline steps.
 -jv (--jenkins-version) VAL : jenkins version to use (only in case 'warDir' is not
                               specified). Defaults to latest LTS.
 -w (--jenkins-war) FILE     : path to exploded jenkins war directory.
 -v (--version)              : Display the current version
 -h (--help)                 : Print this help

where `-a`, `-ns`, `--runHome`, `--runWorkspace` and `-jv` are optional.
```

###  Passing parameters
Any parameter values, for parameters defined on workflow job within `parameters` statement
can be passed to the Jenkinsfile Runner using `-a` or `--arg` switches in key=value format. 

```
$ ./app/target/appassembler/bin/jenkinsfile-runner \
  -w /tmp/jenkins \
  -p /tmp/jenkins_home/plugins \
  -f ~/foo/ \
  # pipeline has two parameters param1 and param2
  -a "param1=Hello" \
  -a "param2=value2"
```

## Usage in Docker

### Execution

Jenkinsfile Runner can be launched simply as...

```
    docker run --rm -v $(pwd)/Jenkinsfile:/workspace/Jenkinsfile jenkins4eval/jenkinsfile-runner
```

Advanced options:

* `JAVA_OPTS` environment variable can be passed to pass extra options to the image
* In the Vanilla `Dockerfile` the master workspace is mapped to `/build`.
  This directory can be exposed as a volume.
  The docker image generated with Custom War Packager maps the workspace to `/build` by default and it can be exposed as well.
  However it is possible to override that directory if both the `-v` docker option and the `--runWorkspace` Jenkinsfile Runner option are specified.
* By default the JENKINS_HOME folder is randomly created and disposed afterwards. With the `--runHome` parameter in combination with the `-v` docker option it is possible to specify a folder.   
  e.g. `docker run -v /local/Jenkinsfile:/workspace/Jenkinsfile -v /local/jenkinsHome:/jenkinsHome ${JENKINSFILE_RUNNER_IMAGE} --runHome /jenkinsHome`

  This way you can access the build metadata in `<jenkinsHome>/jobs/job/builds/1`, like the build.xml, logs, and workflow data, even after the container finished.

* The `-ns` and `-a` options can be specified and passed to the image in the same way as the command line execution.

* You may pass `--cli` to obtain an interactive Jenkins CLI session.

## Docker build

You can build your customized Jenkinsfile Runner image using the Vanilla Dockerfile included in this repository or [with Custom WAR Packager](https://jenkins.io/blog/2018/10/16/custom-war-packager/#jenkinsfile-runner-packaging).
See the demos and the [Packaging into Docker image](DOCKER.md) page for further detail.

### Building the Vanilla image

This repository includes the base image which can be built simply as...

    docker build -t jenkins4eval/jenkinsfile-runner .

During development you can reuse the local machine build instead of doing a full build from scratch

    docker build -t jenkins4eval/jenkinsfile-runner:dev -f Dockerfile-dev .


## Extending Jenkins Runner

Say you want to install a specific plugin (e.g. slack, in order to send notifications to Slack channel ). You can create two files with the following content:
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
Now you have a custom image of jenkins-runner with your own plugins installed!

## Reporting issues

Jenkinsfile Runner [Jenkins JIRA](https://issues.jenkins-ci.org) for tracking of tasks and defects
(project=`JENKINS`, component=`jenkinsfile-runner`).
GitHub issues can be also used to report issues, but it is not recommended.
For JIRA please follow [these guidelines](https://wiki.jenkins.io/display/JENKINS/How+to+report+an+issue) when reporting issues.
If you see a security issue in the component, please follow the [vulnerability reporting guidelines](https://jenkins.io/security/#reporting-vulnerabilities).

* [Open issues in Jenkins JIRa](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20(Open%2C%20%22In%20Progress%22%2C%20Reopened)%20AND%20component%20%3D%20jenkinsfile-runner)
* [Open issues in GitHub](https://github.com/jenkinsci/jenkinsfile-runner/issues)

## Further reading

* [Implementation Note](IMPLEMENTATION.md)
* [Contributing to Jenkinsfile Runner](CONTRIBUTING.md)
* [Architecture overview](DEVELOPER.md)
* Slides: [Under the hood of serverless Jenkins. Jenkinsfile Runner](https://docs.google.com/presentation/d/1y7YnAdnh5WY59g8oIGTsj8sLQ5KXgoV7uUCBkxcTU88/edit?usp=sharing)

