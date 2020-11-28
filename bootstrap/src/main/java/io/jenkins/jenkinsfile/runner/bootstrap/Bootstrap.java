package io.jenkins.jenkinsfile.runner.bootstrap;


import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunCLICommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;
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
        subcommands = {RunJenkinsfileCommand.class, RunCLICommand.class, AutoComplete.GenerateCompletion.class, CommandLine.HelpCommand.class})
public class Bootstrap implements Callable<Integer> {

    @CommandLine.Mixin
    public PipelineRunOptions pipelineRunOptions;

    @CommandLine.Mixin
    public JenkinsLauncherOptions launcherSettings;

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
        RunJenkinsfileCommand command = new RunJenkinsfileCommand();
        command.launcherOptions = launcherSettings;
        command.pipelineRunOptions = pipelineRunOptions;
        return command.call();
    }
}
