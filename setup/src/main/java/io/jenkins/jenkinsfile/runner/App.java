package io.jenkins.jenkinsfile.runner;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.IApp;

/**
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class App implements IApp {
    @Override
    public int run(Bootstrap bootstrap) throws Throwable {
        JenkinsLauncher launcher = createLauncherFor(bootstrap);
        return launcher.launch();
    }

    private JenkinsLauncher createLauncherFor(Bootstrap bootstrap) {
        if(bootstrap.cliOnly) {
            return new CLILauncher(bootstrap);
        } else {
            return new JenkinsfileRunnerLauncher(bootstrap);
        }
    }
}
