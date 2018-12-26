package io.jenkins.jenkinsfile.runner;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletContextListener;

/**
 * Kills off {@link ServletContextListener}s loaded from web.xml.
 *
 * <p>
 * This is so that the launcher can create the {@link jenkins.model.Jenkins} object.
 * with the home directory of our choice.
 *
 * @author Kohsuke Kawaguchi
 */
final class NoListenerConfiguration extends AbstractLifeCycle {
    private final WebAppContext context;

    NoListenerConfiguration(WebAppContext context) {
        this.context = context;
    }

    @Override
    protected void doStart() {
        context.setEventListeners(null);
    }
}
