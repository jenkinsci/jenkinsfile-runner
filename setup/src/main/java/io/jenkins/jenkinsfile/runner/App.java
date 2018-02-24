package io.jenkins.jenkinsfile.runner;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.remoting.Which;
import hudson.tasks.Shell;
import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.IApp;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

/**
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class App implements IApp {
    @Override
    public int run(Bootstrap bootstrap) throws Throwable {
        final int[] returnCode = new int[]{-1};
        JenkinsfileRunnerRule rule = new JenkinsfileRunnerRule(bootstrap.warDir, bootstrap.pluginsDir);
        Statement s = rule.apply(new Statement() {

            private FreeStyleBuild b;

            @Override
            public void evaluate() throws Throwable {
//                URLClassLoader cl = new URLClassLoader(
//                        collectJars(new File(bootstrap.appRepo, "io/jenkins/jenkinsfile-runner-payload"), (File f) -> true, new ArrayList<>()),
//                        rule.jenkins.getPluginManager().uberClassLoader);

                FreeStyleProject p = rule.jenkins.createProject(FreeStyleProject.class, "name");
                p.getBuildersList().add(new Shell("ls -la"));
                QueueTaskFuture<FreeStyleBuild> f = p.scheduleBuild2(0);
                b = f.getStartCondition().get();

                writeLogTo(System.out);

                f.get();    // wait for the completion
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

    /**
     * Recursively scan a directory to find jars
     */
    private List<URL> collectJars(File dir, FileFilter filter, List<URL> jars) throws IOException {
        File[] children = dir.listFiles();
        if (children!=null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    collectJars(child, filter, jars);
                } else {
                    if (child.getName().endsWith(".jar") && filter.accept(child)) {
                        jars.add(child.toURI().toURL());
                    }
                }
            }
        }
        return jars;    // just to make this method flow a bit better
    }
}
