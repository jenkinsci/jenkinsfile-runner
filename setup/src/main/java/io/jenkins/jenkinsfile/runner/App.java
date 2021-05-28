package io.jenkins.jenkinsfile.runner;

import io.jenkins.jenkinsfile.runner.bootstrap.IApp;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsfileCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.LintJenkinsfileCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;

/**
 * App handler for {@link RunJenkinsfileCommand}.
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class App implements IApp {

    @Override
    public int run(JenkinsLauncherCommand command) throws Throwable {
        if (!(command instanceof JenkinsfileCommand)) {
            throw new IllegalStateException(
                    String.format("%s is invoked with a wrong class type. Required=%s, got=%s",
                            App.class, JenkinsfileCommand.class, command.getClass()));
        }

        JenkinsLauncher<?> launcher;
        if (command instanceof RunJenkinsfileCommand) {
            launcher = new JenkinsfileRunnerLauncher((RunJenkinsfileCommand) command);
        } else if (command instanceof LintJenkinsfileCommand) {
            launcher = new JenkinsfileLinterLauncher((LintJenkinsfileCommand) command);
        } else {
            // This is most likely a development time error.
            throw new IllegalArgumentException(String.format("Unrecognised command: %s", command.getClass().getName()));
        }
        return launcher.launch();
    }
}
