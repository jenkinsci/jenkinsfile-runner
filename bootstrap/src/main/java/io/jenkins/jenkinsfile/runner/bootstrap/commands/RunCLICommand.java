package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import picocli.CommandLine;

@CommandLine.Command(name = "cli", description = "Runs interactive Jenkins CLI")
public class RunCLICommand extends JenkinsLauncherCommand {

    @Override
    public String getAppClassName() {
        return "io.jenkins.jenkinsfile.runner.CLIApp";
    }

}
