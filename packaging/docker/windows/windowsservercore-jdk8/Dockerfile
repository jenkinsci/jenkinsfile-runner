# escape=`

ARG WINDOWS_DOCKER_TAG=1809

# Define maven version for other stages
FROM jenkins4eval/maven-windows-jdk-8 as maven

FROM maven as jenkinsfilerunner-mvncache
ADD pom.xml C:/src/pom.xml
ADD app/pom.xml C:/src/app/pom.xml
ADD bootstrap/pom.xml C:/src/bootstrap/pom.xml
ADD setup/pom.xml C:/src/setup/pom.xml
ADD payload/pom.xml C:/src/payload/pom.xml
ADD payload-dependencies/pom.xml C:/src/payload-dependencies/pom.xml
ADD tests/pom.xml C:/src/tests/pom.xml
ADD vanilla-package/pom.xml C:/src/vanilla-package/pom.xml

SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]

WORKDIR C:/src
ENV MAVEN_OPTS=-Dmaven.repo.local=C:/mavenrepo
RUN mvn compile dependency:resolve dependency:resolve-plugins

FROM maven as jenkinsfilerunner-build
ENV MAVEN_OPTS=-Dmaven.repo.local=C:/mavenrepo
COPY --from=jenkinsfilerunner-mvncache C:/mavenrepo C:/mavenrepo
ADD . C:/jenkinsfile-runner
RUN cd C:/jenkinsfile-runner ; mvn package
RUN New-Item -Path C:/app -ItemType Directory | Out-Null ; Copy-Item C:/jenkinsfile-runner/vanilla-package/target/war/jenkins.war C:/jenkinsfile-runner/vanilla-package/target/war/jenkins.zip ; Expand-Archive -Path C:/jenkinsfile-runner/vanilla-package/target/war/jenkins.zip -DestinationPath C:/app/jenkins ; `
  Remove-Item -Force -Recurse -Path "C:/jenkinsfile-runner/vanilla-package/target/war/jenkins.zip","C:/app/jenkins/scripts","C:/app/jenkins/jsbundles","C:/app/jenkins/css","C:/app/jenkins/images","C:/app/jenkins/help","C:/app/jenkins/WEB-INF/detached-plugins","C:/app/jenkins/winstone.jar","C:/app/jenkins/WEB-INF/jenkins-cli.jar","C:/app/jenkins/WEB-INF/lib/jna-4.5.2.jar"

FROM openjdk:8-jdk-windowsservercore-$WINDOWS_DOCKER_TAG
ENV JENKINS_UC https://updates.jenkins.io
#USER Container
RUN New-Item -ItemType Directory -Path "C:/app","C:/ProgramData/jenkins/ref/plugins" | Out-Null
COPY --from=jenkinsfilerunner-build C:/app/jenkins C:/app/jenkins
COPY --from=jenkinsfilerunner-build C:/jenkinsfile-runner/app/target/appassembler C:/app
COPY --from=jenkinsfilerunner-build C:/jenkinsfile-runner/vanilla-package/target/plugins C:/ProgramData/jenkins/ref/plugins
COPY C:/jenkinsfile-runner/packaging/docker/windows/jenkinsfile-runner-launcher.ps1 C:/app/bin

VOLUME C:/build

ENTRYPOINT ["powershell.exe", "-f", "C:/app/bin/jenkinsfile-runner-launcher.ps1", `
            "-w", "C:/app/jenkins", `
            "-p", "C:/ProgramData/jenkins/ref/plugins", `
            "-f", "C:/workspace", `
            "--runWorkspace", "C:/build"]
