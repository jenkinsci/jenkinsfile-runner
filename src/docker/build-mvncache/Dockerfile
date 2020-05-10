# Define maven version for other stages
FROM maven:3.5.4 as maven

FROM maven as jenkinsfilerunner-mvncache
ADD pom.xml /src/pom.xml
ADD app/pom.xml /src/app/pom.xml
ADD bootstrap/pom.xml /src/bootstrap/pom.xml
ADD setup/pom.xml /src/setup/pom.xml
ADD payload/pom.xml /src/payload/pom.xml
ADD payload-dependencies/pom.xml /src/payload-dependencies/pom.xml
ADD vanilla-package/pom.xml /src/vanilla-package/pom.xml

WORKDIR /src
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
# We collect a full cache instead of resolving dependencies
# RUN mvn compile dependency:resolve dependency:resolve-plugins
RUN mvn clean package
