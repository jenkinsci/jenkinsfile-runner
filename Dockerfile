# Define maven version for other stages
FROM maven:3.5.4 as maven

FROM maven as jenkinsfilerunner-mvncache
ADD pom.xml /src/pom.xml
ADD app/pom.xml /src/app/pom.xml
ADD bootstrap/pom.xml /src/bootstrap/pom.xml
ADD setup/pom.xml /src/setup/pom.xml
ADD payload/pom.xml /src/payload/pom.xml
ADD payload-dependencies/pom.xml /src/payload-dependencies/pom.xml
ADD tests/pom.xml /src/tests/pom.xml
ADD vanilla-package/pom.xml /src/vanilla-package/pom.xml

WORKDIR /src
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
RUN mvn compile dependency:resolve dependency:resolve-plugins

FROM maven as jenkinsfilerunner-build
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD . /jenkinsfile-runner
RUN cd /jenkinsfile-runner && mvn package
RUN mkdir /app && unzip /jenkinsfile-runner/vanilla-package/target/war/jenkins.war -d /app/jenkins && \
  rm -rf /app/jenkins/scripts /app/jenkins/jsbundles /app/jenkins/css /app/jenkins/images /app/jenkins/help /app/jenkins/WEB-INF/detached-plugins /app/jenkins/winstone.jar /app/jenkins/WEB-INF/jenkins-cli.jar /app/jenkins/WEB-INF/lib/jna-4.5.2.jar

FROM openjdk:8-jdk
ENV JENKINS_UC https://updates.jenkins.io
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/ref/casc
ENV JENKINS_PM_VERSION 0.1-alpha-10
ENV JENKINS_PM_URL https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/plugin-management-parent-pom-$JENKINS_PM_VERSION/jenkins-plugin-manager-$JENKINS_PM_VERSION.jar
USER root
RUN mkdir -p /app /usr/share/jenkins/ref/plugins /usr/share/jenkins/ref/casc /app/bin \
    && echo "jenkins: {}" >/usr/share/jenkins/ref/casc/jenkins.yaml \
    && wget $JENKINS_PM_URL -O /app/bin/jenkins-plugin-manager.jar

COPY --from=jenkinsfilerunner-build /app/jenkins /app/jenkins
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/vanilla-package/target/plugins /usr/share/jenkins/ref/plugins
COPY jenkinsfile-runner-launcher /app/bin

VOLUME /build
VOLUME /usr/share/jenkins/ref/casc

ENTRYPOINT ["/app/bin/jenkinsfile-runner-launcher", \
            "-w", "/app/jenkins",\
            "-p", "/usr/share/jenkins/ref/plugins",\
            "-f", "/workspace", \
            "--runWorkspace", "/build"]
