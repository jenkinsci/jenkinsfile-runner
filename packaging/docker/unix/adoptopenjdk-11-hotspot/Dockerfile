FROM jenkins/jenkinsfile-runner:build-mvncache as jenkinsfilerunner-mvncache

FROM maven:3.6.3-adoptopenjdk-8 as jenkinsfilerunner-build
RUN apt-get update && apt-get install -y unzip
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD app /jenkinsfile-runner/app
ADD bootstrap /jenkinsfile-runner/bootstrap
ADD payload /jenkinsfile-runner/payload
ADD payload-dependencies /jenkinsfile-runner/payload-dependencies
ADD setup /jenkinsfile-runner/setup
ADD vanilla-package /jenkinsfile-runner/vanilla-package
ADD packaging-parent-resources /jenkinsfile-runner/packaging-parent-resources
ADD packaging-parent-pom /jenkinsfile-runner/packaging-parent-pom
ADD packaging-slim-parent-pom /jenkinsfile-runner/packaging-slim-parent-pom
ADD pom.xml /jenkinsfile-runner/pom.xml
RUN cd /jenkinsfile-runner && mvn clean package --batch-mode -ntp --show-version --errors
# Prepare the Jenkins core
RUN mkdir /app && unzip /jenkinsfile-runner/vanilla-package/target/war/jenkins.war -d /app/jenkins && \
  rm -rf /app/jenkins/scripts /app/jenkins/jsbundles /app/jenkins/css /app/jenkins/images /app/jenkins/help /app/jenkins/WEB-INF/detached-plugins /app/jenkins/WEB-INF/jenkins-cli.jar /app/jenkins/WEB-INF/lib/jna-4.5.2.jar \
# Delete HPI files and use the archive directories instead
RUN echo "Optimizing plugins..." && \
  cd /jenkinsfile-runner/vanilla-package/target/plugins && \
  rm -rf *.hpi && \
  for f in * ; do echo "Exploding $f..." && mv "$f" "$f.hpi" ; done;

FROM adoptopenjdk:11.0.8_10-jdk-hotspot
RUN apt-get update && apt-get install wget && rm -rf /var/lib/apt/lists/*

ENV JDK_11 true

ENV JENKINS_UC https://updates.jenkins.io
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/ref/casc
ENV JENKINS_PM_VERSION 2.5.0
ENV JENKINS_PM_URL https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/${JENKINS_PM_VERSION}/jenkins-plugin-manager-${JENKINS_PM_VERSION}.jar

USER root
RUN mkdir -p /app /usr/share/jenkins/ref/plugins /usr/share/jenkins/ref/casc /app/bin \
    && echo "jenkins: {}" >/usr/share/jenkins/ref/casc/jenkins.yaml \
    && wget $JENKINS_PM_URL -O /app/bin/jenkins-plugin-manager.jar

COPY --from=jenkinsfilerunner-build /app/jenkins /app/jenkins
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/vanilla-package/target/plugins /usr/share/jenkins/ref/plugins
COPY /packaging/docker/unix/jenkinsfile-runner-launcher /app/bin

VOLUME /build
VOLUME /usr/share/jenkins/ref/casc

ENTRYPOINT ["/app/bin/jenkinsfile-runner-launcher"]
