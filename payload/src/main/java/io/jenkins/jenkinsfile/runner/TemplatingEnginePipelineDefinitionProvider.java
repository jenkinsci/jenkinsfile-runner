package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.Credentials;
import hudson.Extension;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import org.apache.commons.io.FileUtils;
import org.boozallen.plugins.jte.job.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.io.File;
import java.io.IOException;

@Extension(optional = true)
public class TemplatingEnginePipelineDefinitionProvider extends PipelineDefinitionProvider {

    static final String definitionName; 

    static{
      definitionName = TemplatingEnginePipelineDefinitionProvider.class.getName();
    }

    public boolean matches(PipelineRunOptions options) {
        return options.isJTE;
    }

    @Override
    public void instrumentJob(WorkflowJob job, PipelineRunOptions runOptions) throws IOException {
        AdHocTemplateFlowDefinitionConfiguration config;
        // check for pipeline configuration
        File pipelineConfig = runOptions.pipelineConfiguration;
        if (runOptions.scm != null){
            SCMContainer scm = SCMContainer.loadFromYAML(runOptions.scm);
            Credentials creds = scm.getCredential();
            if(creds != null){
                scm.addCredentialToStore();
            }
            config = new ScmAdHocTemplateFlowDefinitionConfiguration(scm.getSCM(), pipelineConfig.getName(), runOptions.jenkinsfile.getName());
        } else {
            String pipelineTemplate = FileUtils.readFileToString(runOptions.jenkinsfile, "UTF-8");
            boolean hasPipelineConfig = pipelineConfig.exists();
            String pipelineConfigS = null;
            if(hasPipelineConfig){
                pipelineConfigS = FileUtils.readFileToString(pipelineConfig, "UTF-8");
            }
            config = ConsoleAdHocTemplateFlowDefinitionConfiguration.create(true, pipelineTemplate, hasPipelineConfig, pipelineConfigS);
        }
        AdHocTemplateFlowDefinition flowDef = new AdHocTemplateFlowDefinition(config);
        job.setDefinition(flowDef);
    }
}
