# Contributing to Jenkinsfile Runner

This page provides information about contributing code to the Jenkinsfile Runner codebase.

## Getting started

1. Fork the repository on GitHub
2. Clone the forked repository to your machine
3. Install the development tools. In order to develop Jenkinsfile Runner, you need the following tools:
  * Java Development Kit (JDK) 8.
     - We usually use [OpenJDK](http://openjdk.java.net/) but you can use other JDKs as well.
  * Maven 3.5.3 or above.
  * Any IDE which supports importing Maven projects.
  * Docker.
  * Make.

## Building and Debugging

The build flow for Jenkinsfile Runner is built around Maven and Docker.

Run `mvn clean package` for the project to be just built and `mvn clean package -Denvironment=test` for the project
to be built and the integration tests, based on the [Jenkinsfile Runner Test Framework](https://github.com/jenkinsci/jenkinsfile-runner-test-framework), to be launched.

This will generate an assembly artifact through the `appassembler-maven-plugin` that can be configured and used to run Jenkinsfiles.
In case you are interested in generating a Docker image containing both the assembly and the configuration, see [DOCKER.md](DOCKER.md).

There you will also find information regarding debugging Jenkinsfile Runner. In case you are more interested
in the architecture, see [DEVELOPER.md](DEVELOPER.md).

## Testing changes

Jenkinsfile Runner includes integration tests as a part of the repository.

These tests (`tests` module) take a while even on server-grade machines.
All of them will be launched by the continuous integration instance,
so there is no strict need to run them before proposing a pull request.

In case you want to run them, see the previous section. You could also enter the `tests` directory and execute the `make` command

## Proposing Changes

All proposed changes are submitted and code reviewed using the _GitHub Pull Request_ process.

To submit a pull request:

1. Commit changes and push them to your fork on GitHub.
It is a good practice to create branches instead of pushing to master.
2. In GitHub Web UI click the _New Pull Request_ button
3. Select `jenkinsci/jenkinsfile-runner` as _base fork_ and `master` as `base`, then click _Create Pull Request_
4. Fill in the Pull Request description according to the changes.
5. Click _Create Pull Request_
6. Wait for CI results/reviews, process the feedback.

It is recommended that new features/changes include testing proving the changes are working correctly.

## Continuous Integration

Jenkinsfile Runner uses [ci.jenkins.io](http://ci.jenkins.io) as Continuous Integration server and uses Jenkins Pipeine to run builds.
The code for the build flow is stored in the [Jenkinsfile](Jenkinsfile) in the repository root.

If you want to update that build flow (e.g. "add more checks"),
just submit a pull request.