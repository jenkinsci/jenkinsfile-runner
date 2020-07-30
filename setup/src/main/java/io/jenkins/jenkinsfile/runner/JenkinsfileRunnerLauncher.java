package io.jenkins.jenkinsfile.runner;

import hudson.security.ACL;
import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.ClassLoaderBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Set up of Jenkins environment for executing a single Jenkinsfile.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsfileRunnerLauncher extends JenkinsLauncher {
    private static final String RUNNER_CLASS_NAME = "io.jenkins.jenkinsfile.runner.Runner";

    public JenkinsfileRunnerLauncher(Bootstrap bootstrap) {
        super(bootstrap);
    }

    //TODO: add support of timeout
    /**
     * Launch the Jenkins instance
     * No time out and no output message
     */
    @Override
    protected int doLaunch() throws Exception {
        // so that test code has all the access to the system
        ACL.impersonate(ACL.SYSTEM);

        Class<?> runnerClass;
        try {
            runnerClass = Thread.currentThread().getContextClassLoader().loadClass(RUNNER_CLASS_NAME);
        } catch (ClassNotFoundException ex) {
            runnerClass = getRunnerClassFromJar();
        }
        return (int)runnerClass.getMethod("run", Bootstrap.class).
                invoke(runnerClass.newInstance(), bootstrap);
    }

    private Class<?> getRunnerClassFromJar() throws IOException, ClassNotFoundException {
        ClassLoader cl = new ClassLoaderBuilder(jenkins.getPluginManager().uberClassLoader)
                .collectJars(new File(bootstrap.getAppRepo(), "io/jenkins/jenkinsfile-runner/payload"))
                .make();

        return cl.loadClass(RUNNER_CLASS_NAME);
    }

    @Override
    protected String getThreadName() {
        return "Executing " + env.displayName();
    }
}
