# Just a Makefile for manual testing
.PHONY: all

ARTIFACT_ID = jenkinsfile-runner-demo
VERSION = 256.0-test
CWP_VERSION = 1.3
JENKINSFILE_RUNNER_TAG=jenkins-experimental/jenkinsfile-runner-demo

all: clean build

clean:
	rm -rf tmp
	rm -rf .build
	rm -rf source
	rm -rf out


.copySource:
	rsync -av --exclude='.git' --exclude='demo' --exclude='tests' --exclude='*/target/*' $(shell pwd)/../../ $(shell pwd)/source

.build/cwp-cli-${CWP_VERSION}.jar:
	rm -rf .build
	mkdir -p .build
	wget -O .build/cwp-cli-${CWP_VERSION}.jar https://repo.jenkins-ci.org/releases/io/jenkins/tools/custom-war-packager/custom-war-packager-cli/${CWP_VERSION}/custom-war-packager-cli-${CWP_VERSION}-jar-with-dependencies.jar

.build: .build/cwp-cli-${CWP_VERSION}.jar
	java -jar .build/cwp-cli-${CWP_VERSION}.jar \
	     -configPath packager-config.yml -tmpDir $(shell pwd)/out/tmp -version ${VERSION}

.buildInDocker:
	docker run \
	    -v $(shell pwd)/packager-config.yml:/app/packager-config.yml \
	    -v $(shell pwd)/out:/app/out \
	    -v $(shell pwd)/source:/app/source \
	    -v ~/.m2:/root/.m2 \
	    jenkins/custom-war-packager:${CWP_VERSION} \
	    -configPath /app/packager-config.yml -tmpDir /app/out/tmp -version ${VERSION}

.buildImage:
	docker build -t ${JENKINSFILE_RUNNER_TAG} -f $(shell pwd)/out/tmp/output/Dockerfile $(shell pwd)/out/tmp/output/	

build: .copySource .build .buildImage

# Slow, but it does not need anything except Docker
buildInDocker: .copySource .buildInDocker .buildImage

run:
	docker run --rm -v $(shell pwd)/Jenkinsfile:/workspace/Jenkinsfile ${JENKINSFILE_RUNNER_TAG}
