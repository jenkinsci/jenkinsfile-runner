package io.jenkins.jenkinsfile.runner;

import hudson.model.queue.QueueTaskFuture;
import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
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
        Jenkins j = Jenkins.getInstance();
        WorkflowJob w = j.createProject(WorkflowJob.class, "job");
        w.addProperty(new DurabilityHintJobProperty(FlowDurabilityHint.PERFORMANCE_OPTIMIZED));
        w.setDefinition(new CpsScmFlowDefinition(
                new FileSystemSCM(bootstrap.jenkinsfile.getParentFile()),bootstrap.jenkinsfile.getName()));
        QueueTaskFuture<WorkflowRun> f = w.scheduleBuild2(0,
                new SetJenkinsfileLocation(bootstrap.jenkinsfile));

        b = f.getStartCondition().get();

        writeLogTo(System.out);

        f.get();    // wait for the completion
        return b.getResult().ordinal;
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
