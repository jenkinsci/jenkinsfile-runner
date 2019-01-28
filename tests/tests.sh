#!/bin/bash
set -e

current_directory=$(pwd)
test_framework_directory="$current_directory/jenkinsfile-runner-test-framework"
sh_unit_directory="$test_framework_directory/shunit2"

. $test_framework_directory/init-jfr-test-framework.inc

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
