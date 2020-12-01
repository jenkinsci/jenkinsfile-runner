package io.jenkins.jenkinsfile.runner.bootstrap;

import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherCommand;

/**
 * Signature of the main app code.
 *
 * @author Kohsuke Kawaguchi
 */
public interface IApp {
    int run(JenkinsLauncherCommand command) throws Throwable;
}
