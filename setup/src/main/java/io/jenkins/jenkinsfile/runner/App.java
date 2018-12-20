package io.jenkins.jenkinsfile.runner;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.IApp;

/**
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class App implements IApp {
    @Override
    public int run(Bootstrap bootstrap) throws Throwable {
        JenkinsfileRunnerLauncher launcher = new JenkinsfileRunnerLauncher(bootstrap.warDir, bootstrap.pluginsDir);

        return launcher.launch(bootstrap);
    }
}
