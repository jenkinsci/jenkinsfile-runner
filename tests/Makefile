# Just a Makefile for automatic testing
.PHONY: all

all: clean init test

clean:
	rm -rf .jenkinsfile-runner-test-framework

init:
	# TODO replace with first version of the test framework
	git clone https://github.com/jenkinsci/jenkinsfile-runner-test-framework .jenkinsfile-runner-test-framework && cd .jenkinsfile-runner-test-framework && git checkout 0ed9ea7a43161ad97764db566d52412dd2dab621
	rsync -avq --exclude-from='exclude-rsync.txt' $(shell pwd)/../ $(shell pwd)/.jenkinsfile-runner-test-framework/source
	$(MAKE) -C .jenkinsfile-runner-test-framework

test:
	./tests.sh
	./tests-jdk11.sh