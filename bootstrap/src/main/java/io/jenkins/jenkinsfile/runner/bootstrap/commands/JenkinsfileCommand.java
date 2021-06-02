package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import java.io.File;

/**
 * Super class of Jenkinsfile commands.
 */
public abstract class JenkinsfileCommand extends JenkinsLauncherCommand {

    /**
     * Helper method to validate the Jenkinsfile section of the pipeline options.
     *
     * @param pipelineOptions the pipeline options.
     */
    protected void validateJenkinsfileInput(PipelineOptions pipelineOptions) {
        if (pipelineOptions.jenkinsfile == null) {
            pipelineOptions.jenkinsfile = new File("Jenkinsfile");
        }
        if (!pipelineOptions.jenkinsfile.exists()) {
            System.err.println("no Jenkinsfile in current directory.");
            System.exit(-1);
        }
        if (pipelineOptions.jenkinsfile.isDirectory()) {
            pipelineOptions.jenkinsfile = new File(pipelineOptions.jenkinsfile, "Jenkinsfile");
        }
    }
}
