package io.jenkins.jenkinsfile.runner;

import hudson.Extension;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.yaml.pipeline.PipelineAsYamlScriptFlowDefinition;

import java.io.IOException;

@Extension(ordinal = -1000, optional = true)
public class PipelineAsYAMLDefinitionProvider extends PipelineDefinitionProvider {

    static final String definitionName;

    static {
        definitionName = PipelineAsYAMLDefinitionProvider.class.getName();
    }

    public boolean matches(PipelineRunOptions options) {
        String filepath = options.jenkinsfile.getAbsolutePath();
        return filepath.endsWith("Jenkinsfile.yml") || filepath.endsWith("Jenkinsfile.yaml");
    }

    @Override
    public void instrumentJob(WorkflowJob job, PipelineRunOptions runOptions) throws IOException {

        // We do not support SCM definition here due to https://github.com/jenkinsci/pipeline-as-yaml-plugin/issues/28
        job.setDefinition(new PipelineAsYamlScriptFlowDefinition(
                FileUtils.readFileToString(runOptions.jenkinsfile), !runOptions.noSandBox));
    }
}
