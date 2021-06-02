package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import java.io.IOException;
import picocli.CommandLine;

/**
 * The "lint" command.
 */
@CommandLine.Command(name = "lint", description = "Lints a Jenkinsfile", mixinStandardHelpOptions = true)
public class LintJenkinsfileCommand extends JenkinsfileCommand {

    @CommandLine.Mixin
    public PipelineLintOptions pipelineLintOptions;

    @Override
    public String getAppClassName() {
        return "io.jenkins.jenkinsfile.runner.App";
    }

    public PipelineLintOptions getPipelineLintOptions() {
        return pipelineLintOptions;
    }

    @Override
    public void postConstruct() throws IOException {
        validateJenkinsfileInput(pipelineLintOptions);
        super.postConstruct();
    }
}
