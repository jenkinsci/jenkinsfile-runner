#!/bin/bash
set -e

current_directory=$(pwd)
test_framework_directory="$current_directory/.jenkinsfile-runner-test-framework"
working_directory="$test_framework_directory/work-vanilla"
src_directory="$test_framework_directory/source"

version="256.0-test"
jenkinsfile_runner_tag="jenkins-experimental/vanilla-jenkinsfile-runner-test-image"

. $test_framework_directory/init-jfr-test-framework.inc

oneTimeSetUp() {
  rm -rf "$working_directory"
  mkdir -p "$working_directory"

  cd "$src_directory"
  docker_build_options="--no-cache"
  if [ -n "$JFR_DEVEL" ] ; then
    echo "Running tests against the development image"
    docker_build_options="-f Dockerfile-dev"
  fi
  result=$(docker build -t "$jenkinsfile_runner_tag" $docker_build_options . | grep 'Successfully tagged')
  execution_should_success "$?" "$jenkinsfile_runner_tag" "$result"

  cd "$current_directory"
}

test_scripted_pipeline() {
  result=$(docker run --rm -v "$current_directory/test_resources/vanilla-images/test_scripted_pipeline":/workspace "$jenkinsfile_runner_tag" -a "param1=Hello" -a "param2=value2")
  jenkinsfile_execution_should_succeed "$?" "$result"
  logs_contains "Hello world!" "$result"
  logs_contains "Value for param1: Hello" "$result"
  logs_contains "Value for param2: value2" "$result"
}

test_declarative_pipeline() {
  result=$(docker run --rm -v "$current_directory/test_resources/vanilla-images/test_declarative_pipeline":/workspace "$jenkinsfile_runner_tag" -a "param1=Hello" -a "param2=value2")
  jenkinsfile_execution_should_succeed "$?" "$result"
  logs_contains "Hello world!" "$result"
  logs_contains "Value for param1: Hello" "$result"
  logs_contains "Value for param2: value2" "$result"
}

init_framework
