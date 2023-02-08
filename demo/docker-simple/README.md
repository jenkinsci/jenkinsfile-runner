# Create a JFR from the jenkins/jenkins Docker image

> **WARNING:** This demo is outdated and needs to be renewed to the new version

This tutorial will show you how to use the community maintained [Jenkins container](https://github.com/jenkinsci/docker) and convert it into a working Jenkinsfile-Runner aka JFR.

The main benefit to doing it this way is that you could use the exact same Jenkins container you run in production locally on your development machine. The community image also has plenty of documentation on how to further customise the Jenkins container.

## Getting started

We will first build the image localy and then run this example [job](./Jenkinsfile).

1. Build the JFR image with `docker build -t jfr-test .`
2. Now run our job with `docker run --rm -v $(pwd):/workspace jfr-test`
   ```console
   docker run --rm -v $(pwd):/workspace jfr-test
   WARNING: An illegal reflective access operation has occurred
   WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.$ReflectUtils$2 (file:/usr/share/jenkins/WEB-INF/lib/guice-4.0.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
   WARNING: Please consider reporting this to the maintainers of com.google.inject.internal.cglib.core.$ReflectUtils$2
   WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
   WARNING: All illegal access operations will be denied in a future release
   Started
   Resume disabled by user, switching to high-performance, low-durability mode.
   [Pipeline] Start of Pipeline
   [Pipeline] node
   Running on Jenkins in /tmp/jenkinsfileRunner.tmp/jfr15345197312171626702.run/workspace/job
   [Pipeline] {
   [Pipeline] echo
   Hello World!
   [Pipeline] }
   [Pipeline] // node
   [Pipeline] End of Pipeline
   Finished: SUCCESS
   ```

The [Dockerfile](./Dockerfile) has additional comments to explain in more detail on how it works.

## Converting an existing image

In the example above we started out with a fresh image from `jenkins/jenkins`. It's likely you already have a Docker image that you use in production and that you would like to use locally as a JFR. As long as your production Jenkins was built from `jenkins/jenkins` it can be converted into a JFR with these steps:

1. Build the JFR image - `docker build -t jfr-test --build-arg baseImage=${myProdImage}.` Note: You should replace the `${myProdImage}` with the name and tag of your existing Jenkins image.
2. You can run the JFR image same as before `docker run --rm -v $(pwd):/workspace jfr-test`

It's likely that you will need to pass additional options to the `jenkinsfile-runner` binary for your production Jenkins image to work locally as a JFR. For example, if you are using groovy init scripts, you would want to modify the `ENTRYPOINT` in the [Dockerfile](./Dockerfile) to looks like this:
```Dockerfile
ENTRYPOINT ["/app/bin/jenkinsfile-runner", "-w", "/usr/share/jenkins/", "-p", "/usr/share/jenkins/ref/plugins", "--withInitHooks", "/usr/share/jenkins/ref/init.groovy.d/", "-f"]
```

### I'm not using the jenkins/jenkins image

You can still convert any Jenkins installation, regardless if it's using docker or a physical installation. By calling the `jenkinsfile-runner` binary using the correct paths. For `-w` this will be the path to the exploded `jenkins.war` and `-p` will be the correct path to where you installed in your plugins and will likely be in your `JENKINS_HOME` directory.
