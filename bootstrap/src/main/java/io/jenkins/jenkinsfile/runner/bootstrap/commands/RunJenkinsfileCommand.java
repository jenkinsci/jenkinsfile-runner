package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oleg Nenashev
 * @since TODO
 */
@CommandLine.Command(name="run", description = "Runs Jenkinsfile", mixinStandardHelpOptions = true)
public class RunJenkinsfileCommand extends JenkinsLauncherCommand {

    @CommandLine.Mixin
    public PipelineRunOptions pipelineRunOptions;

    private static final String WORKSPACES_DIR_SYSTEM_PROPERTY = "jenkins.model.Jenkins.workspacesDir";

    @Override
    public String getAppClassName() {
        return "io.jenkins.jenkinsfile.runner.App";
    }

    public PipelineRunOptions getPipelineRunOptions() {
        return pipelineRunOptions;
    }

    @Override
    public void postConstruct() throws IOException {
        if (pipelineRunOptions.jenkinsfile == null) {
            pipelineRunOptions.jenkinsfile = new File("Jenkinsfile");
        }
        if (!pipelineRunOptions.jenkinsfile.exists()) {
            System.err.println("no Jenkinsfile in current directory.");
            System.exit(-1);
        }
        if (pipelineRunOptions.jenkinsfile.isDirectory()) {
            pipelineRunOptions.jenkinsfile = new File(pipelineRunOptions.jenkinsfile, "Jenkinsfile");
        }

        if (pipelineRunOptions.runWorkspace != null){
            if (System.getProperty(WORKSPACES_DIR_SYSTEM_PROPERTY) != null) {
                //TODO(oleg_nenashev): It would have been better to keep it other way, but made it as is to retain compatibility
                System.out.println("Ignoring the --runWorkspace argument, because an explicit System property is set (-D" + WORKSPACES_DIR_SYSTEM_PROPERTY + ")");
            } else {
                System.setProperty(WORKSPACES_DIR_SYSTEM_PROPERTY, pipelineRunOptions.runWorkspace.getAbsolutePath());
            }
        }

        if (pipelineRunOptions.workflowParameters == null){
            pipelineRunOptions.workflowParameters = new HashMap<>();
        }
        for (Map.Entry<String, String> workflowParameter : pipelineRunOptions.workflowParameters.entrySet()) {
            if (workflowParameter.getValue() == null) {
                workflowParameter.setValue("");
            }
        }

        if (pipelineRunOptions.cause != null) {
            pipelineRunOptions.cause = pipelineRunOptions.cause.trim();
            if (pipelineRunOptions.cause.isEmpty())  {
                pipelineRunOptions.cause = null;
            }
        }

        if (pipelineRunOptions.jobName.isEmpty()) {
            pipelineRunOptions.jobName = PipelineRunOptions.DEFAULT_JOBNAME;
        }

        if (pipelineRunOptions.keepUndefinedParameters) {
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");
        }

        super.postConstruct();
    }

}
