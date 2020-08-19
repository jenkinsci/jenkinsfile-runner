# Jenkinsfile Runner Tests

## Running the tests on macOS
The version of shUnit2 we are using does not work with the default bash shell on macOS.  In order to get the tests to run, we recommend upgrading your bash shell to a more modern version by following the instructions below.

+ Use [Homebrew](https://brew.sh/) to install the latest version of bash with the command `brew install bash`
+ To verify the installation, execute the command `which -a bash`  This should list at least two entries, including `/bin/bash` and `/usr/local/bin/bash`
+ Change the first line of [tests/tests.sh](https://github.com/jenkinsci/jenkinsfile-runner/blob/master/tests/tests.sh) from `#!/bin/bash` to `#!/usr/local/bin/bash`

More information on updating your bash shell can be found here [Upgrading bash on macOs](https://itnext.io/upgrading-bash-on-macos-7138bd1066ba)

## Developer mode

Building of production Docker packages may take a while.
For vanilla package tests is possible to use a developer mode to speedup local testing.
Example:

```bash
make clean prepare
JFR_DEVEL=true bash ./tests-vanilla.sh
```
