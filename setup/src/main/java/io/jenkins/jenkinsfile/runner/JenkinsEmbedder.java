/*
 * The MIT License
 *
 * Copyright (c) 2004-2018, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 * Yahoo! Inc., Tom Huybrechts, Olivier Lamy, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.jenkinsfile.runner;

import hudson.ClassicPluginStrategy;
import hudson.CloseProofOutputStream;
import hudson.DNSMultiCast;
import hudson.DescriptorExtensionList;

import hudson.ExtensionList;
import hudson.Functions;
import hudson.Launcher;
import hudson.Main;
import hudson.PluginManager;
import hudson.WebAppMain;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.DownloadService;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Queue;
import hudson.model.RootAction;
import hudson.model.TaskListener;
import hudson.model.UpdateSite;
import hudson.model.User;
import hudson.remoting.Which;
import hudson.tools.ToolProperty;
import hudson.util.PersistedList;
import hudson.util.StreamTaskListener;
import hudson.util.jna.GNUCLibrary;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import io.jenkins.jenkinsfile.runner.util.ExecutionEnvironment;
import io.jenkins.jenkinsfile.runner.util.JenkinsHomeLoader;
import io.jenkins.jenkinsfile.runner.util.JenkinsRecipe;
import io.jenkins.jenkinsfile.runner.util.LenientRunnable;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;

import hudson.init.InitMilestone;

import java.nio.channels.ClosedByInterruptException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import org.kohsuke.stapler.Dispatcher;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.MetaClassLoader;

/**
 * Some code is inherited from JenkinsEmbedder in Jenkins Test Harness
 * @author Stephen Connolly
 * @author Oleg Nenashev
 */
@SuppressWarnings({"deprecation","rawtypes"})
public abstract class JenkinsEmbedder implements RootAction {

    protected ExecutionEnvironment env = new ExecutionEnvironment();

    public Jenkins jenkins;

    protected JenkinsHomeLoader homeLoader = JenkinsHomeLoader.NEW;

    /**
     * TCP/IP port that the server is listening on.
     */
    protected int localPort;
    protected Server server;

    /**
     * Where in the {@link Server} is Jenkins deployed?
     * <p>
     * Just like {@link javax.servlet.ServletContext#getContextPath()}, starts with '/' but doesn't end with '/'.
     */
    public String contextPath = "/jenkins";

    /**
     * {@link Runnable}s to be invoked at {@link #after()} .
     */
    protected List<LenientRunnable> tearDowns = new ArrayList<LenientRunnable>();

    protected List<JenkinsRecipe.Runner> recipes = new ArrayList<JenkinsRecipe.Runner>();

    private PluginManager pluginManager = null;

    private boolean origDefaultUseCache = true;

    public Jenkins getInstance() {
        return jenkins;
    }

    /**
     * Override to set up your specific external resource.
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    public void before() throws Throwable {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            if (h instanceof ConsoleHandler) {
                ((ConsoleHandler) h).setFormatter(new SupportLogFormatter());
            }
        }

        if (Thread.interrupted()) { // JENKINS-30395
            LOGGER.warning("was interrupted before start");
        }

        if(Functions.isWindows()) {
            // JENKINS-4409.
            // URLConnection caches handles to jar files by default,
            // and it prevents delete temporary directories on Windows.
            // Disables caching here.
            // Though defaultUseCache is a static field,
            // its setter and getter are provided as instance methods.
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            origDefaultUseCache = aConnection.getDefaultUseCaches();
            aConnection.setDefaultUseCaches(false);
        }

        env.pin();
        recipe();
        AbstractProject.WORKSPACE.toString();
        User.clear();

        try {
            Field theInstance = Jenkins.class.getDeclaredField("theInstance");
            theInstance.setAccessible(true);
            if (theInstance.get(null) != null) {
                LOGGER.warning("Jenkins.theInstance was not cleared by a previous test, doing that now");
                theInstance.set(null, null);
            }
        } catch (Exception x) {
            LOGGER.log(Level.WARNING, null, x);
        }

        try {
            jenkins = newJenkins();
            // If the initialization graph is corrupted, we cannot expect that Jenkins is in the good shape.
            // Likely it is an issue in @Initializer() definitions (see JENKINS-37759).
            // So we just fail the test.
            if (jenkins.getInitLevel() != InitMilestone.COMPLETED) {
                throw new Exception("Jenkins initialization has not reached the COMPLETED initialization stage. Current state is " + jenkins.getInitLevel() +
                        ". Likely there is an issue with the Initialization task graph (e.g. usage of @Initializer(after = InitMilestone.COMPLETED)). See JENKINS-37759 for more info");
            }
        } catch (Exception e) {
            // if Jenkins instance fails to initialize, it leaves the instance field non-empty and break all the rest of the tests, so clean that up.
            Field f = Jenkins.class.getDeclaredField("theInstance");
            f.setAccessible(true);
            f.set(null,null);
            throw e;
        }
        jenkins.setNoUsageStatistics(true); // collecting usage stats from tests are pointless.

        // TODO: set NOOP API
        // jenkins.setCrumbIssuer(new TestCrumbIssuer());

        jenkins.servletContext.setAttribute("app",jenkins);
        jenkins.servletContext.setAttribute("version","?");
        WebAppMain.installExpressionFactory(new ServletContextEvent(jenkins.servletContext));

        // set a default JDK to be the one that the harness is using.
        jenkins.getJDKs().add(new JDK("default",System.getProperty("java.home")));

        configureUpdateCenter();

        // expose the test instance as a part of URL tree.
        // this allows tests to use a part of the URL space for itself.
        jenkins.getActions().add(this);

        JenkinsLocationConfiguration.get().setUrl(getURL().toString());
    }

    /**
     * Configures the update center setting for the test.
     * By default, we load updates from local proxy to avoid network traffic as much as possible.
     */
    protected void configureUpdateCenter() throws Exception {
        final String updateCenterUrl;
        jettyLevel(Level.INFO);

        // don't waste bandwidth talking to the update center
        DownloadService.neverUpdate = true;
        UpdateSite.neverUpdate = true;

        PersistedList<UpdateSite> sites = jenkins.getUpdateCenter().getSites();
        sites.clear();
        //TODO: Mock UC?
        // sites.add(new UpdateSite("default", updateCenterUrl));
    }

    private static void dumpThreads() {
        ThreadInfo[] threadInfos = Functions.getThreadInfos();
        Functions.ThreadGroupMap m = Functions.sortThreadsAndGetGroupMap(threadInfos);
        for (ThreadInfo ti : threadInfos) {
            System.err.println(Functions.dumpThreadInfo(ti, m));
        }
    }

    //TODO: JVM is going to die for Jenkinsfile Runner, the most of the code here is YAGNI
    /**
     * Override to tear down your specific external resource.
     */
    public void after() throws Exception {
        jettyLevel(Level.WARNING);
        try {
            server.stop();
        } catch (Exception e) {
            // ignore
        } finally {
            jettyLevel(Level.INFO);
        }
        for (LenientRunnable r : tearDowns)
            try {
                r.run();
            } catch (Exception e) {
                // ignore
            }

        if (jenkins!=null)
            jenkins.cleanUp();
        ExtensionList.clearLegacyInstances();
        DescriptorExtensionList.clearLegacyInstances();

        try {
            env.dispose();
        } catch (Exception x) {
            x.printStackTrace();
        }

        // Jenkins creates ClassLoaders for plugins that hold on to file descriptors of its jar files,
        // but because there's no explicit dispose method on ClassLoader, they won't get GC-ed until
        // at some later point, leading to possible file descriptor overflow. So encourage GC now.
        // see http://bugs.sun.com/view_bug.do?bug_id=4950148
        // TODO use URLClassLoader.close() in Java 7
        System.gc();

        // restore defaultUseCache
        if(Functions.isWindows()) {
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            aConnection.setDefaultUseCaches(origDefaultUseCache);
        }
    }

    private static void jettyLevel(Level level) {
        Logger.getLogger("org.eclipse.jetty").setLevel(level);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "self";
    }

    /**
     * Creates a new instance of {@link jenkins.model.Jenkins}. If the derived class wants to create it in a different way,
     * you can override it.
     */
    protected Jenkins newJenkins() throws Exception {
        jettyLevel(Level.WARNING);
        ServletContext webServer = createWebServer();
        File home = homeLoader.allocate();
        setupHome(home);
        // TODO looks like a remaining of Jenkins Test Harness not used at all in the jfr context
        for (JenkinsRecipe.Runner r : recipes) {
            r.decorateHome(this, home);
        }
        try {
            return new Hudson(home, webServer, getPluginManager());
        } catch (InterruptedException x) {
            throw new Exception("Jenkins startup interrupted", x);
        } finally {
            jettyLevel(Level.INFO);
        }
    }

    /**
     * Prepare the newly allocated home with additional files.
     * Currently only Groovy Hooks
     * @param home the target home directory (freshly allocated)
     * @throws IOException any issue during the copy should interrupt the execution
     */
    protected abstract void setupHome(File home) throws IOException;

    public PluginManager getPluginManager() {
        if (jenkins == null) {
            return pluginManager;
        } else {
            return jenkins.getPluginManager();
        }
    }

    /**
     * Sets the {@link PluginManager} to be used when creating a new {@link Jenkins} instance.
     *
     * @param pluginManager
     *      {@code null} to let Jenkins create a new instance of default plugin manager, like it normally does when running as a webapp outside the test.
     */
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        if (jenkins!=null) {
            throw new IllegalStateException("Too late to override the plugin manager");
        }
    }

    public JenkinsEmbedder with(PluginManager pluginManager) {
        setPluginManager(pluginManager);
        return this;
    }

    public File getWebAppRoot() throws Exception {
        return WarExploder.getExplodedDir();
    }

    /**
     * Prepares a webapp hosting environment to get {@link javax.servlet.ServletContext} implementation
     * that we need for testing.
     */
    protected abstract ServletContext createWebServer() throws Exception;

    protected String createUniqueProjectName() {
        return "test"+jenkins.getItems().size();
    }

    /**
     * Creates {@link hudson.Launcher.LocalLauncher}. Useful for launching processes.
     */
    public Launcher.LocalLauncher createLocalLauncher() {
        return new Launcher.LocalLauncher(StreamTaskListener.fromStdout());
    }

    /**
     * Returns the URL of the webapp top page.
     * URL ends with '/'.
     */
    public URL getURL() throws IOException {
        return new URL("http://localhost:"+localPort+contextPath+"/");
    }

    /**
     * Blocks until the ENTER key is hit.
     * This is useful during debugging a test so that one can inspect the state of Jenkins through the web browser.
     */
    public void interactiveBreak() throws Exception {
        System.out.println("Jenkins is running at " + getURL());
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
    }

    /**
     * Pauses the execution until ENTER is hit in the console.
     * <p>
     * This is often very useful so that you can interact with Jenkins
     * from an browser, while developing a test case.
     */
    public void pause() throws IOException {
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
    }

    /**
     * Creates a {@link TaskListener} connected to stdout.
     */
    public TaskListener createTaskListener() {
        return new StreamTaskListener(new CloseProofOutputStream(System.out));
    }

    public void setQuietPeriod(int qp) throws IOException {
        jenkins.setQuietPeriod(qp);
    }

    /**
     * Returns true if Jenkins is building something or going to build something.
     */
    public boolean isSomethingHappening() {
        if (!jenkins.getQueue().isEmpty())
            return true;
        for (Computer n : jenkins.getComputers())
            if (!n.isIdle())
                return true;
        return false;
    }

    /**
     * Waits until Jenkins finishes building everything, including those in the queue.
     */
    public void waitUntilNoActivity() throws Exception {
        waitUntilNoActivityUpTo(Integer.MAX_VALUE);
    }

    /**
     * Waits until Jenkins finishes building everything, including those in the queue, or fail the test
     * if the specified timeout milliseconds is
     */
    public void waitUntilNoActivityUpTo(int timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        int streak = 0;

        while (true) {
            Thread.sleep(10);
            if (isSomethingHappening())
                streak=0;
            else
                streak++;

            if (streak>5)   // the system is quiet for a while
                return;

            if (System.currentTimeMillis()-startTime > timeout) {
                List<Queue.Executable> building = new ArrayList<Queue.Executable>();
                for (Computer c : jenkins.getComputers()) {
                    for (Executor e : c.getExecutors()) {
                        if (e.isBusy())
                            building.add(e.getCurrentExecutable());
                    }
                    for (Executor e : c.getOneOffExecutors()) {
                        if (e.isBusy())
                            building.add(e.getCurrentExecutable());
                    }
                }
                dumpThreads();
                throw new AssertionError(String.format("Jenkins is still doing something after %dms: queue=%s building=%s",
                        timeout, Arrays.asList(jenkins.getQueue().getItems()), building));
            }
        }
    }


//
// recipe methods. Control the test environments.
//

    public abstract void recipe() throws Exception;

    static void decorateHomeFor(File home, List<URL> all) throws Exception {
        List<Jpl> jpls = new ArrayList<Jpl>();
        for (URL hpl : all) {
            Jpl jpl = new Jpl(home, hpl);
            jpl.loadManifest();
            jpls.add(jpl);
        }
        for (Jpl jpl : jpls) {
            jpl.resolveDependencies(jpls);
        }
    }

    private static final class Jpl {
        private final File home;
                final URL jpl;
                Manifest m;
                private String shortName;

                Jpl(File home, URL jpl) {
                    this.home = home;
                    this.jpl = jpl;
                }

                void loadManifest() throws IOException {
                    m = new Manifest(jpl.openStream());
                    shortName = m.getMainAttributes().getValue("Short-Name");
                    if(shortName ==null)
                        throw new Error(jpl +" doesn't have the Short-Name attribute");
                    FileUtils.copyURLToFile(jpl, new File(home, "plugins/" + shortName + ".jpl"));
                }

                void resolveDependencies(List<Jpl> jpls) throws Exception {
                    // make dependency plugins available
                    // TODO: probably better to read POM, but where to read from?
                    // TODO: this doesn't handle transitive dependencies

                    // Tom: plugins are now searched on the classpath first. They should be available on
                    // the compile or test classpath.
                    // For transitive dependencies, we could evaluate Plugin-Dependencies transitively.
                    String dependencies = m.getMainAttributes().getValue("Plugin-Dependencies");
                    if(dependencies!=null) {
                        DEPENDENCY:
                        for( String dep : dependencies.split(",")) {
                            String suffix = ";resolution:=optional";
                            boolean optional = dep.endsWith(suffix);
                            if (optional) {
                                dep = dep.substring(0, dep.length() - suffix.length());
                            }
                            String[] tokens = dep.split(":");
                            String artifactId = tokens[0];
                            String version = tokens[1];

                            for (Jpl other : jpls) {
                                if (other.shortName.equals(artifactId))
                                    continue DEPENDENCY;    // resolved from another JPL file
                            }

                            File dependencyJar=resolveDependencyJar(artifactId,version);
                            if (dependencyJar == null) {
                                if (optional) {
                                    LOGGER.log(Level.INFO, "cannot resolve optional dependency {0} of {1}; skipping", new Object[] {dep, shortName});
                                    continue;
                                }
                                throw new IOException("Could not resolve " + dep + " in " + System.getProperty("java.class.path"));
                            }

                            File dst = new File(home, "plugins/" + artifactId + ".jpi");
                            if(!dst.exists() || dst.lastModified()!=dependencyJar.lastModified()) {
                                try {
                                    FileUtils.copyFile(dependencyJar, dst);
                                } catch (ClosedByInterruptException x) {
                                    throw new Exception("copying dependencies was interrupted", x);
                                }
                            }
                        }
                    }
                }

            private @CheckForNull File resolveDependencyJar(String artifactId, String version) throws Exception {
                // try to locate it from manifest
                Enumeration<URL> manifests = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (manifests.hasMoreElements()) {
                    URL manifest = manifests.nextElement();
                    InputStream is = manifest.openStream();
                    Manifest m = new Manifest(is);
                    is.close();

                    if (artifactId.equals(m.getMainAttributes().getValue("Short-Name")))
                        return Which.jarFile(manifest);
                }

                // For snapshot plugin dependencies, an IDE may have replaced ~/.m2/repository/…/${artifactId}.hpi with …/${artifactId}-plugin/target/classes/
                // which unfortunately lacks META-INF/MANIFEST.MF so try to find index.jelly (which every plugin should include) and thus the ${artifactId}.hpi:
                Enumeration<URL> jellies = getClass().getClassLoader().getResources("index.jelly");
                while (jellies.hasMoreElements()) {
                    URL jellyU = jellies.nextElement();
                    if (jellyU.getProtocol().equals("file")) {
                        File jellyF = new File(jellyU.toURI());
                        File classes = jellyF.getParentFile();
                        if (classes.getName().equals("classes")) {
                            File target = classes.getParentFile();
                            if (target.getName().equals("target")) {
                                File hpi = new File(target, artifactId + ".hpi");
                                if (hpi.isFile()) {
                                    return hpi;
                                }
                            }
                        }
                    }
                }

                return null;
            }

    }

    public JenkinsEmbedder withNewHome() {
        return with(JenkinsHomeLoader.NEW);
    }

    public JenkinsEmbedder withExistingHome(File source) throws Exception {
        return with(new JenkinsHomeLoader.CopyExisting(source));
    }

    /**
     * Declares that this test case expects to start with one of the preset data sets.
     * See {@code test/src/main/preset-data/}
     * for available datasets and what they mean.
     */
    public JenkinsEmbedder withPresetData(String name) {
        name = "/" + name + ".zip";
        URL res = getClass().getResource(name);
        if(res==null)   throw new IllegalArgumentException("No such data set found: "+name);

        return with(new JenkinsHomeLoader.CopyExisting(res));
    }

    public JenkinsEmbedder with(JenkinsHomeLoader homeLoader) {
        this.homeLoader = homeLoader;
        return this;
    }

    /**
     * Sometimes a part of a test case may ends up creeping into the serialization tree of {@link hudson.model.Saveable#save()},
     * so detect that and flag that as an error.
     */
    private Object writeReplace() {
        throw new AssertionError("JenkinsEmbedder " + env.displayName() + " is not supposed to be serialized");
    }

    // needs to keep reference, or it gets GC-ed.
    private static final Logger SPRING_LOGGER = Logger.getLogger("org.springframework");
    private static final Logger JETTY_LOGGER = Logger.getLogger("org.mortbay.log");

    static {
        // screen scraping relies on locale being fixed.
        Locale.setDefault(Locale.ENGLISH);

        {// enable debug assistance, since tests are often run from IDE
            Dispatcher.TRACE = true;
            MetaClass.NO_CACHE = true;
            // load resources from the source dir.
            File dir = new File("src/main/resources");
            if (dir.exists() && MetaClassLoader.debugLoader == null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    try {
                        MetaClassLoader.debugLoader = new MetaClassLoader(
                                new URLClassLoader(new URL[]{dir.toURI().toURL()}));
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                    return null;
                });
            }
        }

        // suppress some logging which we do not much care about here
        SPRING_LOGGER.setLevel(Level.WARNING);
        JETTY_LOGGER.setLevel(Level.WARNING);

        // hudson-behavior.js relies on this to decide whether it's running unit tests.
        Main.isUnitTest = true;

        // remove the upper bound of the POST data size in Jetty.
        System.setProperty("org.mortbay.jetty.Request.maxFormContentSize","-1");
    }

    private static final Logger LOGGER = Logger.getLogger(JenkinsEmbedder.class.getName());

    public static final List<ToolProperty<?>> NO_PROPERTIES = Collections.<ToolProperty<?>>emptyList();

    public static final MimeTypes MIME_TYPES;
    static {
        jettyLevel(Level.WARNING); // suppress Log.initialize message
        try {
            MIME_TYPES = new MimeTypes();
        } finally {
            jettyLevel(Level.INFO);
        }
        MIME_TYPES.addMimeMapping("js","application/javascript");
        Functions.DEBUG_YUI = true;

        // during the unit test, predictably releasing classloader is important to avoid
        // file descriptor leak.
        ClassicPluginStrategy.useAntClassLoader = true;

        // DNS multicast support takes up a lot of time during tests, so just disable it altogether
        // this also prevents tests from falsely advertising Jenkins
        DNSMultiCast.disabled = true;

        try {
            GNUCLibrary.LIBC.unsetenv("MAVEN_OPTS");
            GNUCLibrary.LIBC.unsetenv("MAVEN_DEBUG_OPTS");
        } catch (LinkageError x) {
            // skip; TODO 1.630+ can use Functions.isGlibcSupported
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,"Failed to cancel out MAVEN_OPTS",e);
        }
    }

    //TODO: remove?
    protected abstract LoginService configureUserRealm();

}
