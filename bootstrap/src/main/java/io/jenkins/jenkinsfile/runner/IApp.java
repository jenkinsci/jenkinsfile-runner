package io.jenkins.jenkinsfile.runner;

/**
 * Signature of the App class in the setup module to invoke.
 *
 * @author Kohsuke Kawaguchi
 */
public interface IApp {
    int run(Bootstrap bootstrap) throws Throwable;
}
