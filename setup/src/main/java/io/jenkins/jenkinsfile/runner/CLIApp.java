package io.jenkins.jenkinsfile.runner;

import io.jenkins.jenkinsfile.runner.bootstrap.IApp;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunCLICommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;

/**
 * App handler for {@link RunCLICommand}.
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class CLIApp implements IApp {
    @Override
    public int run(JenkinsLauncherCommand command) throws Throwable {
        if (!(command instanceof RunCLICommand)) {
            throw new IllegalStateException(
                    String.format("%s is invoked with a wrong class type. Required=%s, got=%s",
                            CLIApp.class, RunCLICommand.class, command.getClass()));
        }
        CLILauncher launcher = new CLILauncher((RunCLICommand) command);
        return launcher.launch();
    }
}
