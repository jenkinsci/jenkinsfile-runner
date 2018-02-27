FROM maven:3.5.2 as jenkinsfilerunner-build
ARG JENKINS_VERSION=2.108
ADD . /app
RUN \
  cd app && mvn package \
  && wget http://mirrors.jenkins.io/war/$JENKINS_VERSION/jenkins.war \
  && unzip jenkins.war -d /tmp/jenkins
# This is where we would start Jenkins, login in as administrator and install the default plugins.
# This is a gap intended to be filled with configuration as code
#JENKINS_HOME=/tmp/plugins java -jar jenkins.war

FROM openjdk:8-jdk
RUN mkdir /app 
# For now just copy the plugins from the workspace
COPY plugins /app/plugins
COPY --from=jenkinsfilerunner-build /tmp/jenkins /app/jenkins
COPY app/target/appassembler /app 

ENTRYPOINT ["/app/bin/jenkinsfile-runner", "/app/jenkins", "/app/plugins","/workspace"]
