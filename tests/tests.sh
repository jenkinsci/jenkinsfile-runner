#!/bin/bash

current_directory=$(pwd)
test_framework_directory="$current_directory/jenkinsfile-runner-test-framework"
sh_unit_directory="$test_framework_directory/shunit2"

version="256.0-test"
CWP_version="1.3"
jenkinsfile_runner_tag="jenkins-experimental/jenkinsfile-runner-test-image"

downloaded_cwp_jar="to_update"

. $test_framework_directory/utilities/utils.inc
. $test_framework_directory/utilities/cwp/custom-war-packager.inc
. $test_framework_directory/utilities/jfr/jenkinsfile-runner.inc

oneTimeSetUp() {
  downloaded_cwp_jar=$(download_cwp "$test_framework_directory" "$CWP_version")
  downloaded_cwp_jar=$(remove_string "$downloaded_cwp_jar" "CWP downloaded as ")
}

setUp() {
  echo "Initializing the test case. Ignoring so far"
}

test_with_tag() {
  jfr_tag=$(generate_docker_image_from_jar "$test_framework_directory" "$downloaded_cwp_jar" "$version" "$current_directory/test_resources/test_with_tag/packager-config.yml" "$jenkinsfile_runner_tag" | grep 'Successfully tagged')
  assertEquals "Should retrieve exit code 0" "0" "$?"
  assertContains "Should contain the given tag" "$jfr_tag" "$jenkinsfile_runner_tag"

  result=$(run_jfr_docker_image "$jenkinsfile_runner_tag" "$current_directory/test_resources/test_with_tag/Jenkinsfile")
  assertEquals "Should retrieve exit code 0" "0" "$?"
  assertContains "Should execute the Jenkinsfile successfully" "$result" "[Pipeline] End of Pipeline"
  assertContains "Should execute the Jenkinsfile successfully" "$result" "Finished: SUCCESS"

  result=$(run_jfr_docker_image "$jenkinsfile_runner_tag")
  assertNotEquals "Should not retrieve exit code 0" "0" "$?"
  assertContains "Should retrieve an error message" "$result" "Missing parameters"
}

test_with_default_tag() {
  jfr_tag=$(generate_docker_image_from_jar "$test_framework_directory" "$downloaded_cwp_jar" "$version" "$current_directory/test_resources/test_with_tag/packager-config.yml" | grep 'Successfully tagged')
  assertEquals "Should retrieve exit code 0" "0" "$?"
  assertNotContains "Should not contain the given tag" "$jfr_tag" "$jenkinsfile_runner_tag"
}

test_missing_params() {
  jfr_tag=$(generate_docker_image_from_jar "$test_framework_directory" "$downloaded_cwp_jar" "$version")
  assertNotEquals "Should not retrieve exit code 0" "0" "$?"
  assertContains "Should retrieve an error message" "$jfr_tag" "Missing parameters"
}

oneTimeTearDown() {
  echo "Cleaning the test suite. Ignoring so far"
}

tearDown() {
  echo "Cleaning the test case. Ignoring so far"
}

. $sh_unit_directory/shunit2
