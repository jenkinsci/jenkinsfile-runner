# Jenkinsfile Runner
Jenkinsfile Runner is an experiment to package Jenkins pipeline execution as a command line tool.
The intend use cases include:

* Use Jenkins in Function-as-a-Service context
* Assist editing `Jenkinsfile` locally
* Integration test shared libraries

[CHANGELOG](CHANGELOG.md)

## Usage in command-line
Jenkinsfile Runner can be run in command line or in Docker.
In case you want to run it in command line just follow these steps:

1. Download the jar file available in [artifactory](https://repo.jenkins-ci.org/webapp/#/home) or build the source code from this repository
2. Prepare the execution environment
3. Run the command

### Build
To build this code just use maven as follows:
```
mvn package
```
This will produce the distribution in `app/target/appassembler`.

### Preparation
Find `jenkins.war` that represents the version of Jenkins that you'd like to use,
then unzip it somewhere.
```
wget http://mirrors.jenkins.io/war-stable/latest/jenkins.war
unzip jenkins.war -d /tmp/jenkins
```

Next, create a directory and assemble all the plugins that you'd like to use with the build.
One way to do this is to run Jenkins setup wizard and install the default set of plugins.
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


# Usage: jenkinsfile-runner -w <path to war> -p <path to plugins> -f <path to Jenkinsfile> [-a "param1=Hello" -a "param2=value2"]
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
 --runWorkspace FILE     : Path to the workspace of the run to be used within
                           the node{} context. It applies to both Jenkins
                           master and agents (or side containers) if any.
                           Requires Jenkins 2.119 or above
 -a (--arg)              : Parameters to be passed to workflow job. Use
                           multiple -a switches for multiple params
 -f (--file) FILE        : Path to Jenkinsfile (or directory containing a
                           Jenkinsfile) to run, default to ./Jenkinsfile.
 -ns (--no-sandbox)      : Disable workflow job execution within sandbox
                           environment
 -p (--plugins) FILE     : plugins required to run pipeline. Either a
                           plugins.txt file or a /plugins installation
                           directory. Defaults to plugins.txt.
 -v (--version) VAL      : jenkins version to use. Defaults to latest LTS.
 -w (--jenkins-war) FILE : path to jenkins.war or exploded jenkins war directory
```

where `--runWorkspace`, `-ns` and `-a` are optional.

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
See the demos and the [Packaging into Docker image](DOCKER.md) page for further detail.

### Build the docker image
You can build your customized Jenkinsfile Runner image using the Vanilla Dockerfile included in this repository or [with Custom WAR Packager](https://jenkins.io/blog/2018/10/16/custom-war-packager/#jenkinsfile-runner-packaging)

### Execution
Once the Docker image is built, Jenkinsfile Runner can be launched simply as...

```
    docker run --rm -v $(shell pwd)/Jenkinsfile:/workspace/Jenkinsfile ${JENKINSFILE_RUNNER_IMAGE}
```

Advanced options:

* `JAVA_OPTS` environment variable can be passed to pass extra options to the image
* In the Vanilla `Dockerfile` the master workspace is mapped to `/build`.
  This directory can be exposed as a volume.
  The docker image generated with Custom War Packager maps the workspace to `/build` by default and it can be exposed as well.
  However it is possible to override that directory if both the `-v` docker option and the `--runworkspace` Jenkinsfile Runner option are specified.
* The `-ns` and `-a` options can be specified and passed to the image in the same way as the command line execution.

## Docker build

    docker build -t jenkins/jenkinsfile-runner .

During development you can reuse the local machine build instead of doing a full build from scratch

    docker build -t jenkins/jenkinsfile-runner:dev -f Dockerfile-dev .

## Reporting issues

Jenkinsfile Runner uses [Jenkins JIRA](https://issues.jenkins-ci.org) for tracking of tasks and defects.
(project=`JENKINS`, component=`jenkinsfile-runner).
Please follow [these guidelines](https://wiki.jenkins.io/display/JENKINS/How+to+report+an+issue) when reporting issues.
If you see a security issue in the component, please follow the [vulnerability reporting guidelines](https://jenkins.io/security/#reporting-vulnerabilities).

## Further reading

* [Implementation Note](IMPLEMENTATION.md)
* [Contributing to Jenkinsfile Runner](CONTRIBUTING.md)
* [Architecture overview](DEVELOPER.md)

