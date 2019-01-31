# Jenkinsfile Runner
Jenkinsfile Runner is an experiment to package Jenkins pipeline execution as a command line tool.
The intend use cases include:

* Use Jenkins in Function-as-a-Service context
* Assist editing `Jenkinsfile` locally
* Integration test shared libraries

[CHANGELOG](./CHANGELOG.md)

## Build
Currently there's no released distribution, so you must first build this code:
```
mvn package
```
This will produce the distribution in `app/target/appassembler`.

## Preparation
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

## Usage
Say you have your Git repository checked out at `~/foo` that contains `Jenkinsfile` and your source code.
You can now run Jenkinsfile Runner like this:

### Usage in Docker

See the demos.
Once Docker image is built, Jenkinsfile Runner can be launched simply as...

```
    docker run --rm -v $(shell pwd)/Jenkinsfile:/workspace/Jenkinsfile ${JENKINSFILE_RUNNER_IMAGE}
```

Advanced options:

* `JAVA_OPTS` environment variable can be passed to pass extra options to the image
* In the suggested `Dockerfile` the master workspace is mapped to `/build`.
  This directory can be exposed as a volume.

### Usage in command-line

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

Passing parameters defined within `parameters` section of the pipeline is optional. 


## Demo

* [Building Jenkinsfile Runner with Custom WAR Packager](demo/cwp)

## Further reading

* [Packaging into Docker image](DOCKER.md)
* [Implementation Note](IMPLEMENTATION.md)
* [Building Jenkinsfile Runner images with Custom WAR Packager](https://jenkins.io/blog/2018/10/16/custom-war-packager/#jenkinsfile-runner-packaging)

