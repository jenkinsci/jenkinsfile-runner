# Jenkinsfile Runner Tests

## Running the testst on macOS
The version of shUnit2 we are using does not work with the default bash shell on macOS. 

In order to get tests to run, we recommend upgrading your bash shell by following the instructions here [Upgrading bash on macOs](https://itnext.io/upgrading-bash-on-macos-7138bd1066ba)

Then change the first line in tests/tests.sh from `#!/bin/bash` to `#!/usr/bin/env bash`


