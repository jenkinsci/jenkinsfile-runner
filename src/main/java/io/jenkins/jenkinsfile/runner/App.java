package io.jenkins.jenkinsfile.runner;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.Shell;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;

public class App {
    public static void main(String[] args) throws Throwable {
        System.exit(new App().run());
    }

    public int run() throws Throwable {
        final int[] returnCode = new int[]{-1};
        JenkinsfileRunnerRule rule = new JenkinsfileRunnerRule();
        Statement s = rule.apply(new Statement() {

            private FreeStyleBuild b;

            @Override
            public void evaluate() throws Throwable {
                FreeStyleProject p = rule.jenkins.createProject(FreeStyleProject.class, "name");
                p.getBuildersList().add(new Shell("ls -la"));
                QueueTaskFuture<FreeStyleBuild> f = p.scheduleBuild2(0);
                b = f.getStartCondition().get();

                writeLogTo(System.out);

                f.get();    // wait for the completion
                System.out.println("Completed "+ b.getFullDisplayName()+" : "+ b.getResult());
                returnCode[0] = b.getResult().ordinal;
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
        }, Description.createSuiteDescription("main"));
        s.evaluate();

        return returnCode[0];
    }
}
