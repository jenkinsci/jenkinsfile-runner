# Jenkinsfile Runner Tests

## Running the testst on macOS
The version of shUnit2 we are using does not work with the default bash shell on macOS.  In order to get the tests to run, we recommend upgrading your bash shell to a more modern version by following the instructions below.

+ Use [Homebrew](https://brew.sh/) to install the latest version of bash with the command `brew install bash`
+ To verify the installation, execute the command `which -a bash`  This should list at least two entries, including `/bin/bash` and `/usr/local/bin/bash`
+ Add `/usr/local/bin/bash` to the whitelist of login shells using the command `sudo vim /etc/shells`
+ Set the new shell to be your default shell with the command `chsh -s /usr/local/bin/bash`

More information can be found here [Upgrading bash on macOs](https://itnext.io/upgrading-bash-on-macos-7138bd1066ba)

Finally,  change the first line in `tests/tests.sh` from `#!/bin/bash` to `#!/usr/bin/env bash`


