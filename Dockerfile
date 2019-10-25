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

ARG JENKINS_REF=/usr/share/jenkins/ref
ARG JENKINS_HOME=/app/jenkins
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
COPY --from=jenkinsfilerunner-mvncache /mavenrepo /mavenrepo
ADD . /jenkinsfile-runner
RUN cd /jenkinsfile-runner && mvn package
RUN mkdir /app && \
  unzip /jenkinsfile-runner/vanilla-package/target/war/jenkins.war -d ${JENKINS_HOME} && \
  rm -rf ${JENKINS_HOME}/scripts ${JENKINS_HOME}/jsbundles ${JENKINS_HOME}/css ${JENKINS_HOME}/images ${JENKINS_HOME}/help ${JENKINS_HOME}/WEB-INF/detached-plugins ${JENKINS_HOME}/winstone.jar ${JENKINS_HOME}/WEB-INF/jenkins-cli.jar ${JENKINS_HOME}/WEB-INF/lib/jna-4.5.2.jar

FROM openjdk:8-jdk

ARG JENKINS_REF=/usr/share/jenkins/ref
ARG JENKINS_HOME=/app/jenkins
ENV JENKINS_REF ${JENKINS_REF}
ENV JENKINS_HOME ${JENKINS_HOME}

USER root
RUN mkdir -p /app ${JENKINS_REF}/plugins ${JENKINS_REF}/casc
COPY --from=jenkinsfilerunner-build ${JENKINS_HOME} ${JENKINS_HOME}
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/app/target/appassembler /app
COPY --from=jenkinsfilerunner-build /jenkinsfile-runner/vanilla-package/target/plugins ${JENKINS_REF}/plugins
COPY jenkinsfile-runner-launcher /app/bin

VOLUME /build
VOLUME ${JENKINS_REF}/casc

ENTRYPOINT ["/app/bin/jenkinsfile-runner-launcher"]

CMD ["run"]
