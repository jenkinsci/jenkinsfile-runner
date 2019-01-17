ARG JENKINS_VERSION=2.121.1

# Define maven version for other stages
FROM maven:3.5.2 as maven

FROM maven as jenkinsfilerunner-mvncache
ADD pom.xml /src/pom.xml
ADD app/pom.xml /src/app/pom.xml
ADD bootstrap/pom.xml /src/bootstrap/pom.xml
ADD setup/pom.xml /src/setup/pom.xml
ADD payload/pom.xml /src/payload/pom.xml
ADD payload-dependencies/pom.xml /src/payload-dependencies/pom.xml
ADD test/pom.xml /src/test/pom.xml

WORKDIR /src
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
RUN mvn compile dependency:resolve dependency:resolve-plugins

FROM maven as jenkinsfilerunner-build
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD . /jenkinsfile-runner
RUN cd /jenkinsfile-runner && mvn package

FROM jenkins/jenkins:${JENKINS_VERSION}
USER root
RUN mkdir /app && unzip /usr/share/jenkins/jenkins.war -d /app/jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app

ENTRYPOINT ["/app/bin/jenkinsfile-runner", \
            "-w", "/app/jenkins",\
            "-p", "/usr/share/jenkins/ref/plugins",\
            "-f", "/workspace"]
