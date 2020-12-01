package io.jenkins.jenkinsfile.runner.bootstrap;


import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunCLICommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.VersionCommand;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Main entry point for the Jenkinsfile Runner execution.
 *
 * @author Kohsuke Kawaguchi
 * @author Oleg Nenashev
 */
@Command(name = "jenkinsfile-runner", versionProvider = Util.VersionProviderImpl.class, sortOptions = false, mixinStandardHelpOptions = true,
        subcommands = {RunJenkinsfileCommand.class, RunCLICommand.class, AutoComplete.GenerateCompletion.class, VersionCommand.class, CommandLine.HelpCommand.class})
public class Bootstrap implements Callable<Integer> {

    @CommandLine.Mixin
    public PipelineRunOptions pipelineRunOptions;

    @CommandLine.Mixin
    public JenkinsLauncherOptions launcherOptions;

    /**
     * @deprecated Replaced by {@link RunCLICommand}
     */
    @Deprecated
    @CommandLine.Option(names = "--cli", hidden = true,
            description = "Launch interactive CLI")
    public boolean cliMode;

    public static void main(String[] args) throws Throwable {
        // break for attaching profiler
        if (Boolean.getBoolean("start.pause")) {
            System.console().readLine();
        }

        int exitCode = new CommandLine(new Bootstrap()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Executes {@link RunJenkinsfileCommand} by default
     */
    @Override
    public Integer call() throws IllegalStateException {
        // TODO: Remove it: Compatibility mode for Docker images
        if (cliMode || System.getenv("FORCE_JENKINS_CLI") != null) {
            try {
                System.out.println("WARNING: Using the deprecated CLI mode. Use the 'cli' subcommand instead of passing the argument or an environment variable");
                RunCLICommand command = new RunCLICommand();
                command.launcherOptions = launcherOptions;
                command.postConstruct();
                return command.runJenkinsfileRunnerApp();
            } catch (Throwable ex) {
                throw new RuntimeException("Unhandled exception", ex);
            }
        }

        RunJenkinsfileCommand command = new RunJenkinsfileCommand();
        command.launcherOptions = launcherOptions;
        command.pipelineRunOptions = pipelineRunOptions;
        return command.call();
    }
}
