#!/bin/bash
set -e

current_directory=$(pwd)
test_framework_directory="$current_directory/.jenkinsfile-runner-test-framework"
working_directory="$test_framework_directory/work-negative"

version="256.0-test"
jenkinsfile_runner_valid_tag="jenkins-experimental/jenkinsfile-runner-test-image"
jenkinsfile_runner_invalid_tag="jenkins-experimental/jenkinsfile-runner-test-invalid-image"

. $test_framework_directory/init-jfr-test-framework.inc

oneTimeSetUp() {
  rm -rf "$working_directory"
  mkdir -p "$working_directory"
  downloaded_cwp_jar=$(download_cwp "$working_directory")

  # docker image to test the plugin version collision
  jfr_tag=$(execute_cwp_jar_and_generate_docker_image "$working_directory" "$downloaded_cwp_jar" "$version" "$current_directory/test_resources/negative-scenarios/config/packager-config-plugin-collision.yml" "$jenkinsfile_runner_invalid_tag" | grep 'Successfully tagged')
  execution_should_success "$?" "$jenkinsfile_runner_invalid_tag" "$jfr_tag"
}

setUp() {
  # timeout per test
  if [[ ${_shunit_test_} == *"test_pipeline_execution_hangs"* ]]
  then
    set_timeout 10
  elif [[ ${_shunit_test_} == *"plugin_versions_collision"* ]]
  then
    set_timeout 15
  else
    set_timeout -1
  fi
}

test_plugin_versions_collision() {
  result=$(eval private_execution_after_timeout "$jenkinsfile_runner_invalid_tag" "$current_directory/test_resources/negative-scenarios/test_plugin_versions_collision/Jenkinsfile")
  execution_success "$?"
  logs_contains "Pipeline: Step API v2.10 is older than required" "$result"
}

#function to be used in test where a timeout is expected
private_execution_after_timeout() {
  set +e
  result=$(run_jfr_docker_image "$1" "$2")
  if [ "0" != "$?" ]
  then
    echo "$result"
    # expected that the execution fails because of the timeout
    return 0
  else
    return 1
  fi
  set -e
}

oneTimeTearDown() {
  # force docker termination in case it is still executing
  docker_id=$(docker ps -aqf "name=$jenkinsfile_runner_invalid_tag")
  if [ ! -z "$docker_id" ]
  then
    docker stop -t 1 "$docker_id"
  fi

  # remove docker with invalid configuration
  docker_id=$(docker images -q "$jenkinsfile_runner_invalid_tag")
  if [ ! -z "$docker_id" ]
  then
    docker rmi -f "$docker_id"
  fi
}

init_framework
