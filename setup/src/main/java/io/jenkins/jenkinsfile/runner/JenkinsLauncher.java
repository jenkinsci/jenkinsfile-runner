package io.jenkins.jenkinsfile.runner;

import hudson.ClassicPluginStrategy;
import hudson.util.PluginServletFilter;
import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.util.HudsonHomeLoader;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared behaviour for different modes of launching an embedded Jenkins in the context of jenkinsfile-runner.
 */
public abstract class JenkinsLauncher extends JenkinsEmbedder {
    protected final Bootstrap bootstrap;

    public JenkinsLauncher(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        if(bootstrap.runHome != null) {
            if(!bootstrap.runHome.isDirectory()) {
                throw new IllegalArgumentException("--runHome is not a directory: " + bootstrap.runHome.getAbsolutePath());
            }
            if(bootstrap.runHome.list().length > 0) {
                throw new IllegalArgumentException("--runHome directory is not empty: " + bootstrap.runHome.getAbsolutePath());
            }
            //Override homeLoader to use existing directory instead of creating temporary one
            this.homeLoader = new HudsonHomeLoader.UseExisting(bootstrap.runHome);
        }
    }

    /**
     * Sets up Jetty without any actual TCP port serving HTTP.
     */
    @Override
    protected ServletContext createWebServer() throws Exception {
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(10);
        server = new Server(queuedThreadPool);

        WebAppContext context = new WebAppContext(bootstrap.warDir.getPath(), contextPath);
        context.setClassLoader(getClass().getClassLoader());
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
        context.addBean(new NoListenerConfiguration(context));
        server.setHandler(context);
        context.getSecurityHandler().setLoginService(configureUserRealm());
        context.setResourceBase(bootstrap.warDir.getPath());

        // Jenkins core and some extension points supply extension points which try to access the filter
        // In Jenkins core it is define in web.xml
        // TODO: Consider reusing Jenkins web.xml instead of manual magic
        context.addFilter(PluginServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

        server.start();

        localPort = -1;

        setPluginManager(new PluginManagerImpl(context.getServletContext(), bootstrap.pluginsDir));

        return context.getServletContext();
    }

    public int launch() throws Throwable {
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        currentThread.setName(getThreadName());
        before();
        try {
            return doLaunch();
        } finally {
            after();
            currentThread.setName(originalThreadName);
        }
    }

    /**
     * @return the thread name to use for executing the action
     */
    protected abstract String getThreadName();

    /**
     * Actually launches the Jenkins instance, without any time out or output message.
     * @return the return code for the process
     */
    protected abstract int doLaunch() throws Exception;

    @Override
    public void recipe() {
        // Not action needed so far
    }

    /**
     * Supply a dummy {@link LoginService} that allows nobody.
     */
    @Override
    protected LoginService configureUserRealm() {
        return new JFRLoginService();
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
        // Prevent warnings for plugins with old plugin POM (JENKINS-54425)
        Logger.getLogger(ClassicPluginStrategy.class.getName()).setLevel(Level.SEVERE);
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

    @Override
    protected void setupHome(File home) throws IOException {
        if(bootstrap.withInitHooks != null) {
            if(!bootstrap.withInitHooks.isDirectory()) {
                throw new IllegalArgumentException("--withInitHooks is not a directory: " + bootstrap.withInitHooks.getAbsolutePath());
            }
            if(bootstrap.withInitHooks.list().length == 0) {
                throw new IllegalArgumentException("--withInitHooks directory does not contain any hook: " + bootstrap.withInitHooks.getAbsolutePath());
            }
            FileUtils.copyDirectory(bootstrap.withInitHooks, home.getAbsoluteFile().toPath().resolve("init.groovy.d").toFile());
        }
    }

    private static class JFRLoginService extends AbstractLoginService {

        public JFRLoginService() {
            setName("jfr");
        }

        @Override
        protected String[] loadRoleInfo(UserPrincipal user) {
            return null;
        }

        @Override
        protected UserPrincipal loadUserInfo(String username) {
            return null;
        }
    }
}
