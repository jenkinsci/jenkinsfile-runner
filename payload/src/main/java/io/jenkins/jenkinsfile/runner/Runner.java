package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.CredentialsUnavailableException;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Failure;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.yaml.pipeline.PipelineAsYamlScriptFlowDefinition;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code runs with classloader setup to see all the pipeline plugins loaded
 *
 * @author Kohsuke Kawaguchi
 */
public class Runner {
    private WorkflowRun b;

    /**
     * Main entry point invoked by the setup module
     */
    public int run(Bootstrap bootstrap) throws Exception {
        try {
          Jenkins.checkGoodName(bootstrap.jobName);
        } catch (Failure e) {
          System.err.println(String.format("invalid job name: '%s': %s", bootstrap.jobName, e.getMessage()));
          return -1;
        }
        Jenkins j = Jenkins.getInstance();
        WorkflowJob w = j.createProject(WorkflowJob.class, bootstrap.jobName);
        w.updateNextBuildNumber(bootstrap.buildNumber);
        w.setResumeBlocked(true);
        List<Action> workflowActionsList = new ArrayList<>(3);

        if (bootstrap.jenkinsfile.getName().endsWith(".yml")) {
          // We do not use SCM definition here due to https://github.com/jenkinsci/pipeline-as-yaml-plugin/issues/28
          w.setDefinition(new PipelineAsYamlScriptFlowDefinition(
            FileUtils.readFileToString(bootstrap.jenkinsfile),!bootstrap.noSandBox));
        } else {
            if (bootstrap.scm != null) {
                SCMContainer scm = SCMContainer.loadFromYAML(bootstrap.scm);
                Credentials fromSCM = scm.getCredential();
                if (fromSCM != null) {
                    try {
                        addCredentials(fromSCM);
                    } catch (IOException | CredentialsUnavailableException e) {
                        System.err.printf("could not create credentials: %s%n", e.getMessage());
                        return -1;
                    }
                }
                w.setDefinition(new CpsScmFlowDefinition(scm.getSCM(), bootstrap.jenkinsfile.getName()));
            } else {
                w.setDefinition(new CpsScmFlowDefinition(
                        new FileSystemSCM(bootstrap.jenkinsfile.getParent()), bootstrap.jenkinsfile.getName()));
            }
            workflowActionsList.add(new SetJenkinsfileLocation(bootstrap.jenkinsfile, !bootstrap.noSandBox));
        }

        if (bootstrap.workflowParameters != null && bootstrap.workflowParameters.size() > 0) {
          workflowActionsList.add(createParametersAction(bootstrap));
        }

        if (bootstrap.cause != null) {
          workflowActionsList.add(createCauseAction(bootstrap.cause));
        }

        Action[] workflowActions = workflowActionsList.toArray(new Action[0]);
        workflowActionsList = null;
        QueueTaskFuture<WorkflowRun> f = w.scheduleBuild2(0, workflowActions);

        b = f.getStartCondition().get();

        writeLogTo(System.out);

        f.get();    // wait for the completion
        return b.getResult().ordinal;
    }

    private Action createParametersAction(Bootstrap bootstrap) {
      return new ParametersAction(bootstrap.workflowParameters
            .entrySet()
            .stream()
            .map(e -> new StringParameterValue(e.getKey(), e.getValue()))
            .collect(Collectors.toList()));
    }

    private CauseAction createCauseAction(String cause) {
      Cause c = new JenkinsfileRunnerCause(cause);
      return new CauseAction(c);
    }

    private CredentialsStore getStore() {
        CredentialsStore store = null;
        for (CredentialsStore s : CredentialsProvider.lookupStores(Jenkins.get())) {
            if (s.getProvider() instanceof SystemCredentialsProvider.ProviderImpl) {
                store = s;
                break;
            }
        }
        return store;
    }

    private void writeLogTo(PrintStream out) throws IOException, InterruptedException {
        final int retryCnt = 10;

        // read output in a retry loop, by default try only once
        // writeWholeLogTo may fail with FileNotFound
        // exception on a slow/busy machine, if it takes
        // longish to create the log file
        int retryInterval = 100;
        for (int i=0;i<=retryCnt;) {
            try {
                b.writeWholeLogTo(out);
                break;
            }
            catch (FileNotFoundException | NoSuchFileException e) {
                if ( i == retryCnt ) {
                    throw e;
                }
                i++;
                Thread.sleep(retryInterval);
            }
        }
    }

    private void addCredentials(Credentials creds) throws IOException, CredentialsUnavailableException {
        CredentialsStore store = getStore();
        if (store == null) {
            throw new CredentialsUnavailableException("Credentials specified but could not find credentials store");
        }
        Domain globalDomain = Domain.global();
        store.addCredentials(globalDomain, creds);
    }
}
