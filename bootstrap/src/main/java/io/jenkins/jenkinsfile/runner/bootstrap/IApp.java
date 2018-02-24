package io.jenkins.jenkinsfile.runner.bootstrap;

/**
 * Signature of the main app code.
 *
 * @author Kohsuke Kawaguchi
 */
public interface IApp {
    int run(Bootstrap bootstrap) throws Throwable;
}
