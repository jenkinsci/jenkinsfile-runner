#!/bin/bash
set -e

current_directory=$(pwd)
test_framework_directory="$current_directory/.jenkinsfile-runner-test-framework"
working_directory="$test_framework_directory/work-cwp"

version="256.0-test"
jenkinsfile_runner_tag="jenkins-experimental/jenkinsfile-runner-test-image"
downloaded_cwp_jar="to_update"

. $test_framework_directory/init-jfr-test-framework.inc

oneTimeSetUp() {
  rm -rf "$working_directory"
  mkdir -p "$working_directory"
  downloaded_cwp_jar=$(download_cwp "$working_directory")
  jfr_tag=$(execute_cwp_jar_and_generate_docker_image "$working_directory" "$downloaded_cwp_jar" "$version" "$current_directory/test_resources/cwp-produced-images/config/packager-config.yml" "$jenkinsfile_runner_tag" | grep 'Successfully tagged')
  execution_should_success "$?" "$jenkinsfile_runner_tag" "$jfr_tag"
}

test_cwp() {
  run_jfr_docker_image "$jenkinsfile_runner_tag" "$current_directory/test_resources/cwp-produced-images/test_cwp/Jenkinsfile"
  jenkinsfile_execution_should_succeed "$?"
  logs_contains "Jenkins Evergreen"
}

init_framework
