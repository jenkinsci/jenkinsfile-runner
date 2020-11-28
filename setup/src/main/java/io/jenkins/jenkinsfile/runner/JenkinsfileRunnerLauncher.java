package io.jenkins.jenkinsfile.runner;

import hudson.security.ACL;
import io.jenkins.jenkinsfile.runner.bootstrap.ClassLoaderBuilder;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;

import java.io.File;
import java.io.IOException;

/**
 * Set up of Jenkins environment for executing a single Jenkinsfile.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsfileRunnerLauncher extends JenkinsLauncher<RunJenkinsfileCommand> {
    private static final String RUNNER_CLASS_NAME = "io.jenkins.jenkinsfile.runner.Runner";

    public JenkinsfileRunnerLauncher(RunJenkinsfileCommand command) {
        super(command);
    }

    //TODO: add support of timeout
    /**
     * Launch the Jenkins instance
     * No time out and no output message
     */
    @Override
    protected int doLaunch() throws Exception {
        final JenkinsLauncherOptions launcherOptions = command.getLauncherOptions();
        // so that test code has all the access to the system
        ACL.impersonate(ACL.SYSTEM);
        Class<?> c = command.hasClass(RUNNER_CLASS_NAME)? Class.forName(RUNNER_CLASS_NAME) : getRunnerClassFromJar();
        return (int)c.getMethod("run", PipelineRunOptions.class).invoke(c.newInstance(), command.pipelineRunOptions);
    }

    private Class<?> getRunnerClassFromJar() throws IOException, ClassNotFoundException {
        ClassLoader cl = new ClassLoaderBuilder(jenkins.getPluginManager().uberClassLoader)
                .collectJars(new File(command.getAppRepo(), "io/jenkins/jenkinsfile-runner/payload"))
                .make();

        return cl.loadClass(RUNNER_CLASS_NAME);
    }

    @Override
    protected String getThreadName() {
        return "Executing " + env.displayName();
    }
}
