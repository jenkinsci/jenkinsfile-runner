#!/bin/bash
set -e

current_directory=$(pwd)
test_framework_directory="$current_directory/.jenkinsfile-runner-test-framework"
working_directory="$test_framework_directory/work-scm"

version="256.0-test"
jenkinsfile_runner_tag="jenkins-experimental/jenkinsfile-runner-test-image-scm"
downloaded_cwp_jar="to_update"

. $test_framework_directory/init-jfr-test-framework.inc

test_scm() {
  rm -rf "$working_directory"
  mkdir -p "$working_directory"
  downloaded_cwp_jar=$(download_cwp "$working_directory")
  jfr_tag=$(execute_cwp_jar_and_generate_docker_image "$working_directory" "$downloaded_cwp_jar" "$version" "$current_directory/test_resources/cwp-produced-images/config/packager-config-scm.yml" "$jenkinsfile_runner_tag" | grep 'Successfully tagged')
  execution_should_success "$?" "$jenkinsfile_runner_tag" "$jfr_tag"

  run_jfr_docker_image_with_jfr_options "$jenkinsfile_runner_tag" "$current_directory/test_resources/cwp-produced-images/test_scm/Jenkinsfile" "--xml-scm=$current_directory/test_resources/cwp-produced-images/test_scm/scmconfig.xml"
  jenkinsfile_execution_should_succeed "$?"
  logs_contains "README.md exists with content 'Test repository"
}

init_framework
