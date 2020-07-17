# Change Log - Archve for Old versions

| WARNING: Changelogs have been moved to [GitHub Releases](https://github.com/jenkinsci/jenkinsfile-runner/releases) |
| --- |

## New releases

See [GitHub Releases](https://github.com/jenkinsci/jenkinsfile-runner/releases).

## [1.0-beta-8] - 2019-05-18

### 🚀 New features and improvements

* Update jenkins core to 2.164.2, and update to recent plugins and libs (#115) @oleg-nenashev
* Introduced --runHome to specify the runs JENKINS_HOME (#106) @alxsap
* [JENKINS-55642](https://issues.jenkins-ci.org/browse/JENKINS-55642) - Create experimental "vanilla" Docker packages for the Vanilla version of Jenkinsfile Runner (#99) @fcojfernandez
* [JENKINS-55227](https://issues.jenkins-ci.org/browse/JENKINS-55227) - Add a debug flag to Jenkinsfile Runner (#87) @fcojfernandez

### 📝 Documentation updates

* [JENKINS-56858](https://issues.jenkins-ci.org/browse/JENKINS-56858) - Clarify help text. (#104) @varyvol
* Expand information about the joint use of Jenkinsfile Runner, Custom WAR Packager and Dependabot (#100) @fcojfernandez
* [JENKINS-55663](https://issues.jenkins-ci.org/browse/JENKINS-55663) - Extend the Jenkinsfile Runner user documentation (#96) @fcojfernandez
* [JENKINS-55227](https://issues.jenkins-ci.org/browse/JENKINS-55227) - Extends debug documentation (#95) @varyvol
* [JENKINS-55664](https://issues.jenkins-ci.org/browse/JENKINS-55664) - Include developer documentation (#92) @varyvol
* README: Document the issue reporting guidelines (#86) @oleg-nenashev

### 📦 Dependency updates

* Bump maven-hpi-plugin from 3.3 to 3.5 (#90, #117) @dependabot

### 🚦 Internal changes

* Using lib-support-log-formatter (#107) @jglick
* [JENKINS-55994](https://issues.jenkins-ci.org/browse/JENKINS-55994) - Automated test more specific for java 11 (#102) @varyvol
* Add a Dependabot configuration file (#93) @oleg-nenashev

## [1.0-beta-7] - 2019-02-20

New features and bugfixes:

- [[PR #26](https://github.com/jenkinsci/jenkinsfile-runner/pull/26)]
  Allow passing Pipeline job parameters from CLI 
  * Example: `java -jar jfr-cli.jar ... -a "param1=Hello" -a "param2=value2"`
- [[PR #26](https://github.com/jenkinsci/jenkinsfile-runner/pull/26)]
  Allow disabling [Script Security](https://jenkins.io/doc/book/managing/script-approval/) to allow unrestricted Pipelines and improved performance... at your own risk
  * Example: `java -jar jfr-cli.jar ... --no-sandbox`
- [[JENKINS-56079](https://issues.jenkins-ci.org/browse/JENKINS-56079)] Print the classpath in case the wrong directories are provided to Bootstrap

Internal changes:

- [[JENKINS-54352](https://issues.jenkins-ci.org/browse/JENKINS-54352)] Add functional tests
- [[PR #64](https://github.com/jenkinsci/jenkinsfile-runner/pull/64)] Add README with instructions for running tests on macOS

## [1.0-beta-6] - 2019-01-28

New features:

- [[JENKINS-54426](https://issues.jenkins-ci.org/browse/JENKINS-54426)]
Add support of Running Jenkinsfile Runner with Java 11
([demo](https://github.com/jenkinsci/jenkinsfile-runner/tree/master/demo/cwp-jdk11))
- [[JENKINS-55703](https://issues.jenkins-ci.org/browse/JENKINS-55703)]
Add a new  `—runWorkspace`parameter to define a custom workspace for the build in `node {...}` closures
- [[JENKINS-55703](https://issues.jenkins-ci.org/browse/JENKINS-55703)]
Jenkinsfile Runner Docker image now uses `/build` as a workspace in `node {...}` closures,
it can be mapped to a volume

Bugfixes and improvements:

- [[JENKINS-54353](https://issues.jenkins-ci.org/browse/JENKINS-54353)]
Replace the internal JUnit-based execution engine by a more efficient implementation
- [[JENKINS-54425](https://issues.jenkins-ci.org/browse/JENKINS-54425)]
Avoid warnings about old plugin harness on the Jenkinfle execution
- [[JENKINS-55239](https://issues.jenkins-ci.org/browse/JENKINS-55239)]
Prevent warning about FilesystemSCM not being annotated properly for Pipelines with a `checkout scm` call
- [[JENKINS-55703](https://issues.jenkins-ci.org/browse/JENKINS-55703)]
Change name of the default build workspace directory so that it does not overlap with Jenkins tests 
  * New default path: `${TMP_DIR}/jenkinsfileRunner.tmp/jfr${Unique ID}.run/workspace/job`

Internal changes:

* [[JENKINS-54391](https://issues.jenkins-ci.org/browse/JENKINS-54391)]
Introduce a first version of the 
[Jenkinsfile Runner Test Framework](https://github.com/jenkinsci/jenkinsfile-runner-test-framework/)
* [[PR #50](https://github.com/jenkinsci/jenkinsfile-runner/pull/50)] 
Align Git tags with release version numbers, there is no "parent-" prefix anymore

## [1.0-beta-5]

The release has been burned

## [1.0-beta-4] - 2018-12-18
- [[JENKINS-54375](https://issues.jenkins-ci.org/browse/JENKINS-54375)] Fix bug after changing Maven coordinates.

## [1.0-beta-3] - 2018-12-17
- [[JENKINS-54375](https://issues.jenkins-ci.org/browse/JENKINS-54375)] Exclude Javadoc generation in build.

## [1.0-beta-2] - 2018-12-17
- [[JENKINS-54375](https://issues.jenkins-ci.org/browse/JENKINS-54375)] Fix Javadoc error and include Javadoc generation in build.

## [1.0-beta-1] - 2018-12-17
- [[JENKINS-54375](https://issues.jenkins-ci.org/browse/JENKINS-54375)] First release.

## [1.0-alpha-1] - 2018-11-08
- Not a tagged version but [commit c041d2c4e1c14bf93e1a219d9de4a661bd5fa8ae](https://github.com/jenkinsci/jenkinsfile-runner/commit/c041d2c4e1c14bf93e1a219d9de4a661bd5fa8ae)
- [[JENKINS-54424](https://issues.jenkins-ci.org/browse/JENKINS-54424)] Remove dependency on JTH framework.
- [[JENKINS-54351](https://issues.jenkins-ci.org/browse/JENKINS-54351)] Maven enforcer executed when building.
- [[JENKINS-54485](https://issues.jenkins-ci.org/browse/JENKINS-54485)] Add integration tests based on CWP demo to Jenkinsfile.
