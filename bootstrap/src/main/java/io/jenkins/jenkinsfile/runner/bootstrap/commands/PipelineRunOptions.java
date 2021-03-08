package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;

// TODO: Split Pipeline and generic options?
/**
 * Contains options required for a Jenkins build run.
 */
public class PipelineRunOptions {

    /**package**/ static final String DEFAULT_JOBNAME = "job";

    /**
     * Checked out copy of the working space.
     */
    @CommandLine.Option(names = { "-f", "--file" },
            description = "Path to Jenkinsfile or directory containing a Jenkinsfile, defaults to ./Jenkinsfile")
    public File jenkinsfile;

    /**
     * Workspace for the Run
     */
    @CheckForNull
    @CommandLine.Option(names = "--runWorkspace",
            description = "Path to the workspace of the run to be used within the node{} context. " +
                    "It applies to both Jenkins master and agents (or side containers) if any. " +
                    "Requires Jenkins 2.119 or above")
    public File runWorkspace;



    /**
     * Job name for the Run.
     */
    @CommandLine.Option(names = { "-n", "--job-name"},
            description = "Name of the job the run belongs to")
    public String jobName = DEFAULT_JOBNAME;

    /**
     * Cause of the Run.
     */
    @CommandLine.Option(names = { "-c", "--cause"},
            description = "Cause of the run")
    public String cause;

    /**
     * BuildNumber of the Run.
     */
    @CommandLine.Option(names = { "-b", "--build-number"},
            description = "Build number of the run")
    public int buildNumber = 1;

    @CommandLine.Option(names = { "-a", "--arg" },
            description = "Parameters to be passed to the build. Use multiple -a switches for multiple params")
    @CheckForNull
    public Map<String,String> workflowParameters;

    @CommandLine.Option(names = { "-ns", "--no-sandbox" },
            description = "Disable workflow job execution within sandbox environment")
    public boolean noSandBox;

    @CommandLine.Option(names = { "-u", "--keep-undefined-parameters" },
            description = "Keep undefined parameters if set")
    public boolean keepUndefinedParameters = false;

    @CommandLine.Option(names = "--scm",
            description = "YAML definition of the SCM, with optional credentials, to use for the project")
    public File scm;

    @CommandLine.Option(names = { "-jte", "--jenkins-templating-engine" },
            description = "Use the Jenkins Templating Engine for the build")
    public boolean isJTE = false;

    @CommandLine.Option(names = { "-pc", "--pipeline-configuration" },
            description = "The Pipeline Configuration File when using the Jenkins Templating Engine")
    public File pipelineConfiguration;
}
