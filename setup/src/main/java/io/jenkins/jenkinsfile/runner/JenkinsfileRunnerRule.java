package io.jenkins.jenkinsfile.runner;

import hudson.ClassicPluginStrategy;
import hudson.security.ACL;
import jenkins.slaves.DeprecatedAgentProtocolMonitor;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set up of Jenkins environment for executing a single Jenkinsfile.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsfileRunnerRule extends JenkinsEmbedder {
    private final File warDir;
    private final File pluginsDir;
    /**
     * Keep the reference around to prevent them from getting GCed.
     */
    private final Set<Object> noGc = new HashSet<>();

    public JenkinsfileRunnerRule(File warDir, File pluginsDir) {
        this.warDir = warDir;
        this.pluginsDir = pluginsDir;
    }

    /**
     * Sets up Jetty without any actual TCP port serving HTTP.
     */
    @Override
    protected ServletContext createWebServer() throws Exception {
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(10);
        server = new Server(queuedThreadPool);

        WebAppContext context = new WebAppContext(warDir.getPath(), contextPath);
        context.setClassLoader(getClass().getClassLoader());
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
        context.addBean(new NoListenerConfiguration(context));
        server.setHandler(context);
        context.getSecurityHandler().setLoginService(configureUserRealm());
        context.setResourceBase(warDir.getPath());

        server.start();

        localPort = -1;

        setPluginManager(new PluginManagerImpl(context.getServletContext(),pluginsDir));

        return context.getServletContext();
    }

    /**
     * Supply a dummy {@link LoginService} that allows nobody.
     */
    @Override
    protected LoginService configureUserRealm() {
        return new HashLoginService();
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
        Logger.getLogger(ClassicPluginStrategy.class.getName()).setLevel(Level.SEVERE);
        Logger l = Logger.getLogger(DeprecatedAgentProtocolMonitor.class.getName());
        l.setLevel(Level.OFF);
        noGc.add(l);    // the configuration will be lost if Logger gets GCed.
    }

    /**
     * Skips the clean up.
     *
     * This was initially motivated by SLF4J leaving gnarly messages.
     * The whole JVM is going to die anyway, so we don't really care about cleaning up anything nicely.
     */
    @Override
    public void after() throws Exception {
        jenkins = null;
        super.after();
    }

    //TODO: add support of timeout
    /**
     * No time out and no output message
     */
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testDescription = description;
                Thread t = Thread.currentThread();
                String o = t.getName();
                t.setName("Executing "+ testDescription.getDisplayName());
                before();
                try {
                    // so that test code has all the access to the system
                    ACL.impersonate(ACL.SYSTEM);
                    base.evaluate();
                } finally {
                    after();
                    testDescription = null;
                    t.setName(o);
                }
            }
        };
    }


    // Doesn't work as intended
//    @Initializer(before= InitMilestone.PLUGINS_LISTED)
//    public static void init() {
//        // no external connectivity needed
//        Jenkins.getInstance().setAgentProtocols(Collections.emptySet());
//    }
}
