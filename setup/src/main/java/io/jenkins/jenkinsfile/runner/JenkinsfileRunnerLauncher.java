package io.jenkins.jenkinsfile.runner;

import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;
import java.io.IOException;

/**
 * Set up of Jenkins environment for executing a single Jenkinsfile.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsfileRunnerLauncher extends JenkinsLauncher<RunJenkinsfileCommand> {
    private static final String RUNNER_CLASS_NAME = "io.jenkins.jenkinsfile.runner.Runner";
    private static final String PIPELINE_JOB_CLASS_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";

    public JenkinsfileRunnerLauncher(RunJenkinsfileCommand command) {
        super(command);
    }

    //TODO: add support of timeout
    /**
     * Launch the Jenkins instance
     * No time out and no output message
     */
    @Override
    protected int doLaunch() throws Exception {
        // So that the payload code has all the access to the system
        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            // We are either in the shared environment (uberjar, repo with plugins) where we can already classload the Runner class directly.
            // Or not, and then we consult with the Jenkins core loader and plugin uber classloader
            Class<?> c = command.hasClass(PIPELINE_JOB_CLASS_NAME) ? Class.forName(RUNNER_CLASS_NAME) : getRunnerClassFromJar();
            return (int) c.getMethod("run", PipelineRunOptions.class).invoke(c.getConstructor().newInstance(), command.pipelineRunOptions);
        }
    }

    private Class<?> getRunnerClassFromJar() throws IOException, ClassNotFoundException {
        return getClassFromJar(RUNNER_CLASS_NAME);
    }
}
