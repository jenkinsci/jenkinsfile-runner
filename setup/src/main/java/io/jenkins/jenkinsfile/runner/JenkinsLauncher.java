package io.jenkins.jenkinsfile.runner;

import hudson.ClassicPluginStrategy;
import hudson.PluginManager;
import hudson.util.PluginServletFilter;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherOptions;
import io.jenkins.jenkinsfile.runner.util.JenkinsHomeLoader;
import jenkins.model.Jenkins;
import jenkins.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
public abstract class JenkinsLauncher<T extends JenkinsLauncherCommand> extends JenkinsEmbedder {

    protected final T command;

    private static final Logger LOGGER = Logger.getLogger(JenkinsLauncher.class.getName());

    public JenkinsLauncher(T command) {
        this.command = command;
        final JenkinsLauncherOptions launcherOptions = command.getLauncherOptions();
        if (launcherOptions.jenkinsHome != null) {
            String[] list = launcherOptions.jenkinsHome.list();
            if (!launcherOptions.jenkinsHome.isDirectory() || list == null) {
                throw new IllegalArgumentException("--runHome is not a directory: " + launcherOptions.jenkinsHome.getAbsolutePath());
            }
            if (list.length > 0) {
                throw new IllegalArgumentException("--runHome directory is not empty: " + launcherOptions.jenkinsHome.getAbsolutePath());
            }

            //Override homeLoader to use existing directory instead of creating temporary one
            this.homeLoader = new JenkinsHomeLoader.UseExisting(launcherOptions.jenkinsHome.getAbsoluteFile());
        }
    }

    @Override
    protected Jenkins newJenkins() throws Exception {
        final Jenkins j = super.newJenkins();
        // Notify the bootstrap about the plugin classloader to be used in its logic
     //   command.setPluginClassloader(j.getPluginManager().uberClassLoader);
        return j;
    }

    /**
     * Sets up Jetty without any actual TCP port serving HTTP.
     */
    @Override
    protected ServletContext createWebServer() throws Exception {
        final JenkinsLauncherOptions launcherOptions = command.getLauncherOptions();

        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(10);
        server = new Server(queuedThreadPool);

        WebAppContext context = new WebAppContext(launcherOptions.warDir.getPath(), contextPath);
        context.setClassLoader(getClass().getClassLoader());
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
        context.addBean(new NoListenerConfiguration(context));
        server.setHandler(context);
        context.getSecurityHandler().setLoginService(configureUserRealm());
        context.setResourceBase(launcherOptions.warDir.getPath());

        // Jenkins core and some extension points supply extension points which try to access the filter
        // In Jenkins core it is define in web.xml
        // TODO: Consider reusing Jenkins web.xml instead of manual magic
        context.addFilter(PluginServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

        server.start();

        localPort = -1;

        String pluginManagerClass = SystemProperties.getString(PluginManager.CUSTOM_PLUGIN_MANAGER);
        if (pluginManagerClass == null) {
            // Standard plugin manager for JFR
            setPluginManager(new PluginManagerImpl(context.getServletContext(), launcherOptions.pluginsDir));
        } else {
            LOGGER.log(Level.INFO, "Will use a custom plugin manager {0}. " +
                    "Note that the --pluginsDir option is not used in this case. ", pluginManagerClass);
            setPluginManager(null);
        }

        return context.getServletContext();
    }

    //TODO: add support of timeout
    /**
     * Launches the Jenkins instance, without any time out or output message.
     * @return the return code for the process
     */
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
     * Launches the payload.
     * @return Exit code
     * @throws Exception Any error
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
        if (System.getProperty("java.util.logging.config.file") == null) {
            Logger.getLogger("").setLevel(Level.WARNING);
            // Prevent warnings for plugins with old plugin POM (JENKINS-54425)
            Logger.getLogger(ClassicPluginStrategy.class.getName()).setLevel(Level.SEVERE);
        } else {
            // Rely on the user-supplied logging configuration
            LOGGER.log(Level.INFO, "Will not override system loggers, because the 'java.util.logging.config.file' system property is set");
        }
    }

    @Override
    public void after() throws Exception {
        if (command.launcherOptions.skipShutdown) {
            // Skips the clean up. This was initially motivated by SLF4J leaving gnarly messages.
            // The whole JVM is going to die anyway, but without the cleanup the Jenkins termination logic won't be invoked
            // It may lead to issues in plugins which rely on the shutdown logic (agent connectors, async event streaming, etc.)
            jenkins = null;
        }
        super.after();
    }

    @Override
    protected void setupHome(File home) throws IOException {
        final JenkinsLauncherOptions launcherOptions = command.getLauncherOptions();
        if (launcherOptions.withInitHooks != null) {
            String[] list = launcherOptions.withInitHooks.list();
            if (!launcherOptions.withInitHooks.isDirectory() || list == null) {
                throw new IllegalArgumentException("--withInitHooks is not a directory: " + launcherOptions.withInitHooks.getAbsolutePath());
            }
            if (list.length == 0) {
                throw new IllegalArgumentException("--withInitHooks directory does not contain any hook: " + launcherOptions.withInitHooks.getAbsolutePath());
            }
            FileUtils.copyDirectory(launcherOptions.withInitHooks, home.getAbsoluteFile().toPath().resolve("init.groovy.d").toFile());
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
