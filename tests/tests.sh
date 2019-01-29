#!/usr/bin/env bash
set -e

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
  echo "Initializing the test suit. Ignoring so far"
}

setUp() {
  echo "Initializing the test case. Ignoring so far"
}

test_example() {
  echo "Executing a test. Test nothing. It is only used to create structure"
}

oneTimeTearDown() {
  echo "Cleaning the test suite. Ignoring so far"
}

tearDown() {
  echo "Cleaning the test case. Ignoring so far"
}

. $sh_unit_directory/shunit2
