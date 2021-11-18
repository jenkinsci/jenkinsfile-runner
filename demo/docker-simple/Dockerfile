# I had trouble using jdk8
ARG baseImage=jenkins/jenkins:latest-jdk11

# Load the JFR docker image
FROM jenkins/jenkinsfile-runner:latest as jfr

# Lets configure our Jenkins image
FROM ${baseImage}

# This installs the "Pipeline" plugins
RUN jenkins-plugin-cli --plugins workflow-aggregator

# Now lets turn our existing docker container into a JFR.

# The jenkins/jenkins containers run as the user "jenkins" which will prevent us from exploding files later.
USER root

# copy the files needed to run the JFR binary
COPY --from=jfr /app /app

# We need to explode the jenkins.war for JFR
RUN cd /usr/share/jenkins && jar -xvf jenkins.war

# I change the home
# ENV JENKINS_HOME="/usr/share/jenkins/ref/"

# "--withInitHooks", "/usr/share/jenkins/ref/init.groovy.d/"

ENTRYPOINT ["/app/bin/jenkinsfile-runner", "-w", "/usr/share/jenkins/", "-p", "/usr/share/jenkins/ref/plugins", "-f"]

CMD ["/workspace/Jenkinsfile"]
