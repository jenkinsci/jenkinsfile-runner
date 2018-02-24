package io.jenkins.jenkinsfile.runner;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Hudson;
import jenkins.model.Jenkins;
import jenkins.slaves.DeprecatedAgentProtocolMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ThreadPoolImpl;
import org.jvnet.hudson.test.WarExploder;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set up of Jenkins environment for executing a single Jenkinsfile.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsfileRunnerRule extends JenkinsRule {

    private Logger l2;

    /**
     * Sets up Jetty without any actual TCP port serving HTTP.
     */
    @Override
    protected ServletContext createWebServer() throws Exception {
        server = new Server(new ThreadPoolImpl(new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Jetty Thread Pool");
                return t;
            }
        })));

        WebAppContext context = new WebAppContext(WarExploder.getExplodedDir().getPath(), contextPath);
        context.setClassLoader(getClass().getClassLoader());
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
        context.addBean(new NoListenerConfiguration(context));
        server.setHandler(context);

        context.setResourceBase(WarExploder.getExplodedDir().getPath());

        server.start();

        localPort = -1;

        return context.getServletContext();
    }

    @Override
    public void before() throws Throwable {
        setLogLevels();
        super.before();
    }

    /**
     * We don't want to clutter console with log messages, so kill of any unimportant ones.
     */
    private void setLogLevels() {
        Logger.getLogger("").setLevel(Level.WARNING);
        l2 = Logger.getLogger(DeprecatedAgentProtocolMonitor.class.getName());
        l2.setLevel(Level.OFF);
    }

    // Doesn't work as intended
//    @Initializer(before= InitMilestone.PLUGINS_LISTED)
//    public static void init() {
//        // no external connectivity needed
//        Jenkins.getInstance().setAgentProtocols(Collections.emptySet());
//    }
}
