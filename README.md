# Jenkinsfile Runner
Jenkinsfile Runner is an experiment to package Jenkins pipeline execution as a command line tool.
The intend use cases include:

* Use Jenkins in Function-as-a-Service context
* Assist editing `Jenkinsfile` locally

## Build
Currently there's no released distribution, so you must first build this code:
```
mvn package
```
This will produce the distribution in `app/target/appassembler`.

Alternatively, you build via [Docker](#docker).

## Preparation
Find `jenkins.war` that represents the version of Jenkins that you'd like to use,
then unzip it somewhere.
```
wget http://mirrors.jenkins.io/war/latest/jenkins.war
unzip jenkins.war -d /tmp/jenkins
```

Next, create a directory and assembls all the plugins that you'd like to use with the build.
One way to do this is to run Jenkins setup wizard and install the default set of plugins.
This is a gap intended to be filled with [configuration as code](https://github.com/jenkinsci/configuration-as-code-plugin)
```
JENKINS_HOME=/tmp/plugins java -jar jenkins.war
# go to http://localhost:8080/, follow the installation step
# and install the recommended set of plugins
```

## Usage
Say you have your Git repository checked out at ``~/foo` that contains `Jenkinsfile` and your source code.
You can now run Jenkinsfile Runner like this:

```
$ cat ~/foo/Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Hello world!'
                sh 'ls -la'
            }
        }
    }
}

# Usage: jenkinsfile-runner <path to war> <path to plugins> <path to ws>
$ ./app/target/appassembler/jenkinsfile-runner /tmp/jenkins /tmp/plugins ~/foo
Started
Running in Durability level: PERFORMANCE_OPTIMIZED
Running on Jenkins in /tmp/jenkinsTests.tmp/jenkins8090792616816810094test/workspace/job
[Pipeline] node
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Declarative: Checkout SCM)
[Pipeline] checkout
[Pipeline] }
[Pipeline] // stage
[Pipeline] stage
[Pipeline] { (Build)
[Pipeline] echo
Hello world!
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

## Docker

There is a simple `Dockerfile` that allows for building jenkinsfile-runner. However, you still have
to run the [Preparation](#preparation) steps as mentioned above and provide the `plugins` directory next to the `Dockerfile`. Then you can build the image like so:

```
docker build -t jenkinsfile-runner:1.0-SNAPSHOT-2.108
```

Note that the build will take some minutes.

### Usage of docker image

You then just mount your directory `~/foo` that contains `Jenkinsfile` to a folder within the container and pass this folder as argument:

```
docker run -v~/foo:/workspace jenkinsfile-runner:1.0-SNAPSHOT-2.108 /workspace
```

If you need additional plugins later, you will have to perform the [Preparation](#preparation) steps again :-/. However, you can still use your exiting docker image by mounting the `jenkins` and `plugins` folder to the container:
```
docker run -v /your/jenkins:/app/jenkins -v /your/plugins:/app/plugins -v~/foo:/workspace jenkinsfile-runner:1.0-SNAPSHOT-2.108 /workspace
```