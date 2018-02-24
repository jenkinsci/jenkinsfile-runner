package io.jenkins.jenkinsfile.runner;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.Shell;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class App {
    public static void main(String[] args) throws Throwable {
        System.exit(new App().run());
    }

    public int run() throws Throwable {
        final int[] returnCode = new int[]{-1};
        JenkinsfileRunnerRule rule = new JenkinsfileRunnerRule();
        Statement s = rule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                FreeStyleProject p = rule.jenkins.createProject(FreeStyleProject.class, "name");
                p.getBuildersList().add(new Shell("ls -la"));
                QueueTaskFuture<FreeStyleBuild> f = p.scheduleBuild2(0);
                FreeStyleBuild b = f.getStartCondition().get();
                b.writeWholeLogTo(System.out);

                f.get();    // wait for the completion
                System.out.println("Completed "+b.getFullDisplayName()+" : "+b.getResult());
                returnCode[0] = b.getResult().ordinal;
            }
        }, Description.createSuiteDescription("main"));
        s.evaluate();

        return returnCode[0];
    }
}
