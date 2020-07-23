FROM jenkins4eval/jenkinsfile-runner:build-mvncache as jenkinsfilerunner-mvncache

FROM maven:3.5.4 as jenkinsfilerunner-build
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD app /jenkinsfile-runner/app
ADD bootstrap /jenkinsfile-runner/bootstrap
ADD payload /jenkinsfile-runner/payload
ADD payload-dependencies /jenkinsfile-runner/payload-dependencies
ADD setup /jenkinsfile-runner/setup
ADD vanilla-package /jenkinsfile-runner/vanilla-package
ADD pom.xml /jenkinsfile-runner/pom.xml
RUN cd /jenkinsfile-runner && mvn clean package
RUN mkdir /app && unzip /jenkinsfile-runner/vanilla-package/target/war/jenkins.war -d /app/jenkins && \
  rm -rf /app/jenkins/scripts /app/jenkins/jsbundles /app/jenkins/css /app/jenkins/images /app/jenkins/help /app/jenkins/WEB-INF/detached-plugins /app/jenkins/winstone.jar /app/jenkins/WEB-INF/jenkins-cli.jar /app/jenkins/WEB-INF/lib/jna-4.5.2.jar

# Unofficial experimental image, AdoptOpenJDK does not offer official Alpine for JDK8
FROM adoptopenjdk/openjdk11:jre-11.0.7_10-alpine
ENV JENKINS_UC https://updates.jenkins.io
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/ref/casc
ENV JENKINS_PM_VERSION 0.1-alpha-10
ENV JENKINS_PM_URL https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/plugin-management-parent-pom-$JENKINS_PM_VERSION/jenkins-plugin-manager-$JENKINS_PM_VERSION.jar
USER root
RUN apk add --update --no-cache wget git \
    && mkdir -p /app /usr/share/jenkins/ref/plugins /usr/share/jenkins/ref/casc /app/bin \
    && echo "jenkins: {}" >/usr/share/jenkins/ref/casc/jenkins.yaml \
    && wget $JENKINS_PM_URL -O /app/bin/jenkins-plugin-manager.jar \
    && apk del wget

COPY --from=jenkinsfilerunner-build /app/jenkins /app/jenkins
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/vanilla-package/target/plugins /usr/share/jenkins/ref/plugins
COPY /packaging/docker/unix/jenkinsfile-runner-launcher /app/bin

VOLUME /build
VOLUME /usr/share/jenkins/ref/casc

ENTRYPOINT ["/app/bin/jenkinsfile-runner-launcher"]
