package io.jenkins.jenkinsfile.runner;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsUnavailableException;
import hudson.init.Terminator;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Executor;
import hudson.model.Failure;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.job.properties.DurabilityHintJobProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
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

        for (String jobPathName : jobPathNames) {
            try {
                Jenkins.checkGoodName(jobPathName);
            } catch (Failure e) {
                System.err.printf("invalid job name: '%s': %s%n", jobPathName, e.getMessage());
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
        w.addProperty(new DurabilityHintJobProperty(FlowDurabilityHint.PERFORMANCE_OPTIMIZED));

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

        if (!runOptions.noBuildLogs) {
          writeLogTo(System.out, runOptions.writeLogInitTimeoutSeconds);
        }

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
            System.err.printf("Error creating folder '%s':%s%n", folderName, ex.getMessage());
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

    private void writeLogTo(PrintStream out, int timeoutSeconds) throws IOException, InterruptedException {
        // read output in a retry loop,
        // writeWholeLogTo may fail with FileNotFound
        // exception on a slow/busy machine, if it takes
        // longish to create the log file
        int retryInterval = 100;
        long timeoutMillis = timeoutSeconds * 1000;
        long startTime = System.currentTimeMillis(); 
        while (true) {
            try {
                b.writeWholeLogTo(out);
                break;
            }
            catch (FileNotFoundException | NoSuchFileException e) {
                if ( System.currentTimeMillis() - startTime > timeoutMillis ) {
                    throw e;
                }
                Thread.sleep(retryInterval);
            }
        }
    }

    @Terminator
    public static void stopBuilds() throws Exception {
        for (WorkflowJob p : Jenkins.get().allItems(WorkflowJob.class)) {
            for (WorkflowRun b : p.getBuilds()) {
                Executor exec = b.getExecutor();
                if (exec != null) {
                    exec.interrupt();
                }
            }
        }
    }

}
