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
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import com.cloudbees.hudson.plugins.folder.Folder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This code runs with classloader setup to see all the pipeline plugins loaded
 *
 * @author Kohsuke Kawaguchi
 */
public class Runner {
    private WorkflowRun b;

    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());

    /**
     * Main entry point invoked by the setup module
     */
    public int run(PipelineRunOptions runOptions) throws Exception { 
        String[] jobPathNames = runOptions.jobName.split("/");
        
        for (int i=0; i < jobPathNames.length; i++) {
            try {
                Jenkins.checkGoodName(jobPathNames[i]);
            } catch (Failure e) {
                System.err.println(String.format("invalid job name: '%s': %s", jobPathNames[i], e.getMessage()));
                return -1;
            } 
        }

        Folder folderInScope = null;
        WorkflowJob w = null;       

        //create Folder structure
        for (int i=0; i < jobPathNames.length -1; i++) {
            folderInScope = createOrReturnFolder(folderInScope, jobPathNames[i]);
        }

        //add Pipeline to Folder
        if (folderInScope!=null) {
            w = folderInScope.createProject(WorkflowJob.class, jobPathNames[jobPathNames.length -1]);
        } else {
            w = Jenkins.get().createProject(WorkflowJob.class, jobPathNames[jobPathNames.length -1]);
        }     

        w.updateNextBuildNumber(runOptions.buildNumber);
        w.setResumeBlocked(true);
        List<Action> pipelineActions = new ArrayList<>(3);

        boolean foundProvider = false;
        for (PipelineDefinitionProvider runner : PipelineDefinitionProvider.all()) {
            try {
                if (runner.matches(runOptions)) {
                    runner.instrumentJob(w, runOptions);
                    foundProvider = true;
                    break;
                }
            } catch (Exception ex) {
                throw new Exception("Runner Implementation failed: " + runner.getClass(), ex);
            }
        }

        if (!foundProvider) { // Create default version
            if (runOptions.scm != null) {
                SCMContainer scm = SCMContainer.loadFromYAML(runOptions.scm);
                Credentials fromSCM = scm.getCredential();
                if (fromSCM != null) {
                    try {
                        scm.addCredentialToStore();
                    } catch (IOException | CredentialsUnavailableException e) {
                        return -1;
                    }
                }
                w.setDefinition(new CpsScmFlowDefinition(scm.getSCM(), runOptions.jenkinsfile.getName()));
            } else {
                w.setDefinition(new CpsScmFlowDefinition(
                        new FileSystemSCM(runOptions.jenkinsfile.getParent()), runOptions.jenkinsfile.getName()));
            }
            pipelineActions.add(new SetJenkinsfileLocation(runOptions.jenkinsfile, !runOptions.noSandBox));
        }

        if (runOptions.workflowParameters != null && runOptions.workflowParameters.size() > 0) {
          pipelineActions.add(createParametersAction(runOptions));
        }

        if (runOptions.cause != null) {
          pipelineActions.add(createCauseAction(runOptions.cause));
        }

        Action[] workflowActions = pipelineActions.toArray(new Action[0]);
        QueueTaskFuture<WorkflowRun> f = w.scheduleBuild2(0, workflowActions);

        b = f.getStartCondition().get();

        writeLogTo(System.out);

        f.get();    // wait for the completion
        return b.getResult().ordinal;
    }

    private Folder createOrReturnFolder(Folder addToFolder, String folderName) throws IOException {        
        try {
            Jenkins j = Jenkins.get();

            if(addToFolder==null) {
                Folder folder = j.getItem(folderName, j, Folder.class);
                return  folder!=null ? folder : j.createProject(Folder.class, folderName);
            }

            Folder folder = j.getItem(folderName, addToFolder.getItemGroup(), Folder.class);
            return folder !=null ? folder : addToFolder.createProject(Folder.class, folderName);
        } catch (IOException ex) {
            System.err.println(String.format("Error creating folder '%s':%s", folderName, ex.getMessage()));
            throw ex;
        }   
    }   
    
    private Action createParametersAction(PipelineRunOptions runOptions) {
      return new ParametersAction(runOptions.workflowParameters
            .entrySet()
            .stream()
            .map(e -> new StringParameterValue(e.getKey(), e.getValue()))
            .collect(Collectors.toList()));
    }

    private CauseAction createCauseAction(String cause) {
      Cause c = new JenkinsfileRunnerCause(cause);
      return new CauseAction(c);
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
}
