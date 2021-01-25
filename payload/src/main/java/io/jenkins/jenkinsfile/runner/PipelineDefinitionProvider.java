package io.jenkins.jenkinsfile.runner;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

/**
 * Allows specifying custom Runner implementations
 */
@Restricted(Beta.class)
public abstract class PipelineDefinitionProvider implements ExtensionPoint {

    /**
     * Check whether the extension point can provide a Pipeline for the request.
     * @param options run options
     * @return {@code true} if the extension can produce the Pipeline definition
     */
    public abstract boolean matches(PipelineRunOptions options);

    /**
     * Configures the Pipeline job.
     *
     * Can be overridden if there is no need to update the entire Pipeline.
     * The extension may pass additional configurations and actions.
     *
     * @param job Job to be updated
     * @throws Exception modification failed
     */
    public abstract void instrumentJob(WorkflowJob job, PipelineRunOptions args) throws Exception;

    /**
     * Get all extension instances.
     */
    public static ExtensionList<PipelineDefinitionProvider> all() {
        return ExtensionList.lookup(PipelineDefinitionProvider.class);
    }
}
