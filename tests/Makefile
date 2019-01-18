# Just a Makefile for automatic testing
.PHONY: all

all: clean init test

clean:
	rm -rf jenkinsfile-runner-test-framework

init:
	git clone --depth 1 https://github.com/jenkinsci/jenkinsfile-runner-test-framework
	rsync -avq --exclude-from='exclude-rsync.txt' $(shell pwd)/../ $(shell pwd)/jenkinsfile-runner-test-framework/source
	$(MAKE) -C jenkinsfile-runner-test-framework

test:
	./tests.sh