ARG JENKINS_VERSION=2.108
FROM maven:3.5.2 as jenkinsfilerunner-build
ADD . /jenkinsfile-runner
RUN cd /jenkinsfile-runner && mvn package

FROM jenkins/jenkins:${JENKINS_VERSION}
USER root
RUN mkdir /app && unzip /usr/share/jenkins/jenkins.war -d /app/jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app 

ENTRYPOINT ["/app/bin/jenkinsfile-runner", "/app/jenkins", "/usr/share/jenkins/ref/plugins", "/workspace"]
