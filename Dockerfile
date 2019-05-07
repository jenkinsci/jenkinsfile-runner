ARG JENKINS_VERSION=2.164.2

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

WORKDIR /src
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
RUN mvn compile dependency:resolve dependency:resolve-plugins

FROM maven as jenkinsfilerunner-build
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD . /jenkinsfile-runner
RUN cd /jenkinsfile-runner && mvn package

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
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app
COPY jenkinsfile-runner-launcher /app/bin

VOLUME /build

ENTRYPOINT ["/app/bin/jenkinsfile-runner-launcher", \
            "-w", "/app/jenkins",\
            "-p", "/usr/share/jenkins/ref/plugins",\
            "-f", "/workspace", \
            "--runWorkspace", "/build"]
