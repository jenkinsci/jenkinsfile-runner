package io.jenkins.jenkinsfile.runner;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.ClassLoaderBuilder;
import io.jenkins.jenkinsfile.runner.bootstrap.IApp;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

/**
 * This code runs after Jetty and Jenkins classloaders are set up correctly.
 */
public class App implements IApp {
    @Override
    public int run(Bootstrap bootstrap) throws Throwable {
        final int[] returnCode = new int[]{-1};
        JenkinsfileRunnerRule rule = new JenkinsfileRunnerRule(bootstrap.warDir, bootstrap.pluginsDir);
        Statement s = rule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // insert the payload
                ClassLoader cl = new ClassLoaderBuilder(rule.jenkins.getPluginManager().uberClassLoader)
                        .collectJars(new File(bootstrap.appRepo, "io/jenkins/jenkinsfile-runner-payload"))
                        .make();

                Class<?> c = cl.loadClass("io.jenkins.jenkinsfile.runner.Runner");
                returnCode[0] = (int)c.getMethod("run",Bootstrap.class).invoke(c.newInstance(),bootstrap);
            }
        }, Description.createSuiteDescription("main"));

        s.evaluate();

        return returnCode[0];
    }
}
