# Build the docker image using the local build in developer box
# To avoid downloading everything from the internet and using developer's cache

ARG JENKINS_VERSION=2.150.3

FROM jenkins/jenkins:${JENKINS_VERSION} as jenkins
USER root
# Delete big files not needed
RUN mkdir /app && unzip /usr/share/jenkins/jenkins.war -d /app/jenkins && \
  rm -rf /app/jenkins/scripts /app/jenkins/jsbundles /app/jenkins/css /app/jenkins/images /app/jenkins/help /app/jenkins/WEB-INF/detached-plugins /app/jenkins/winstone.jar /app/jenkins/WEB-INF/jenkins-cli.jar /app/jenkins/WEB-INF/lib/jna-4.5.2.jar

FROM openjdk:8-jdk
ENV JENKINS_UC https://updates.jenkins.io
USER root
RUN mkdir -p /app /usr/share/jenkins/ref/plugins
COPY --from=jenkins /app/jenkins /app/jenkins
COPY --from=jenkins /usr/local/bin/install-plugins.sh /usr/local/bin/install-plugins.sh
COPY --from=jenkins /usr/local/bin/jenkins-support /usr/local/bin/jenkins-support
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
COPY app/target/appassembler /app

ENTRYPOINT ["/app/bin/jenkinsfile-runner", \
            "-w", "/app/jenkins",\
            "-p", "/usr/share/jenkins/ref/plugins",\
            "-f", "/workspace"]
