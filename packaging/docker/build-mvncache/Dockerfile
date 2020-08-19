FROM maven:3.5.4
WORKDIR /src
ENV MAVEN_OPTS=-Dmaven.repo.local=/mavenrepo
ADD . /src
# We collect a full cache instead of resolving dependencies
# RUN mvn compile dependency:resolve dependency:resolve-plugins
RUN cd /src && mvn clean package -Dmaven.test.failure.ignore=true
