# Jenkinsfile Runner Architecture overview

This page provides a brief overview of the Jenkinsfile Runner architecture.

There are 6 modules:
  * `app`
  * `bootstrap`
  * `payload`
  * `payload-dependencies`
  * `setup`
  * `tests`: contains the integration tests powered by the [Jenkinsfile Runner Test Framework](https://github.com/jenkinsci/jenkinsfile-runner-test-framework).
plus the `demo` folder, which contains some examples, especially demonstrating the
integration with [Custom WAR Packer](https://github.com/jenkinsci/custom-war-packager/).

## `app` module

Picks what's necessary from other modules by adding them as dependencies and configures
the `appassembler-maven-plugin` so that an assembly is generated, containing the Jenkinsfile Runner as an
executable script.

## `bootstrap` module

Contains special classloader configuration so that Jenkinsfile Runner is able to work both with Jenkins classes and with the
own project classes. It also contains the `Bootstrap` class:
  * The `main()` for the Jenkinsfile Runner application.
  * Configures the execution based on the values of the provided parameters.
  * Launches the execution.

## `payload` module

Contains the configuration for the "real" pipeline job to be configured and run on the ephemeral Jenkins instance. It basically:
  * Creates a Pipeline job configured to optimize performance.
  * Sets the Jenkinsfile to be executed.
  * Sets the specified parameters.
  * (Un)Sets the script security plugin security layer.
  * Schedules a build and wait for the finalisation, redirecting the logs to the system output.

## `payload-dependencies` module

The `payload` module needs to depend on the presence of many other plugins,
but they are only used during the compilation and therefore are not shipped to runtime.

Having all the dependencies under payload-dependencies makes it easier to exclude
them from assembly.

## `setup` module

It implements the `JenkinsfileRunnerLauncher` in charge of starting the Jenkins instance and calling the Jenkinsfile Runner
execution engine, configuring the classloader properly.

It also contains classes to implement or configure this "environment", such as a custom implementation for the Plugin Manager
or a WAR exploder in charge of exploding the WAR file and fully initialising an instance.
