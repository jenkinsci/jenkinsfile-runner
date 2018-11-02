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
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Functions;
import hudson.Launcher;
import hudson.Main;
import hudson.PluginManager;
import hudson.Util;
import hudson.WebAppMain;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.DownloadService;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.JDK;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.UpdateSite;
import hudson.model.User;
import hudson.model.View;
import hudson.remoting.Which;
import hudson.security.ACL;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tools.ToolProperty;
import hudson.util.PersistedList;
import hudson.util.ReflectionUtils;
import hudson.util.StreamTaskListener;
import hudson.util.jna.GNUCLibrary;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import io.jenkins.jenkinsfile.runner.util.EndOfTestListener;
import io.jenkins.jenkinsfile.runner.util.HudsonHomeLoader;
import io.jenkins.jenkinsfile.runner.util.JenkinsRecipe;
import io.jenkins.jenkinsfile.runner.util.LenientRunnable;
import io.jenkins.jenkinsfile.runner.util.MockFolder;
import io.jenkins.jenkinsfile.runner.util.SupportLogFormatter;
import io.jenkins.jenkinsfile.runner.util.TestEnvironment;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Password;
import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;
import org.junit.internal.AssumptionViolatedException;
import static org.junit.matchers.JUnitMatchers.containsString;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import hudson.init.InitMilestone;
import hudson.model.Job;
import hudson.model.queue.QueueTaskFuture;

import java.nio.channels.ClosedByInterruptException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import jenkins.model.ParameterizedJobMixIn;
import org.hamcrest.core.IsInstanceOf;
import org.junit.rules.Timeout;
import org.junit.runners.model.TestTimedOutException;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.Dispatcher;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.MetaClassLoader;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Some code is inherited from JenkinsRule in Jenkins Test Harness
 * @author Stephen Connolly
 * @author Oleg Nenashev
 */
@SuppressWarnings({"deprecation","rawtypes"})
public abstract class JenkinsRule implements RootAction {

    protected TestEnvironment env;

    protected Description testDescription;

    public Jenkins jenkins;

    protected HudsonHomeLoader homeLoader = HudsonHomeLoader.NEW;

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

    /**
     * Number of seconds until the test times out.
     */
    public int timeout = Integer.getInteger("jenkins.test.timeout", System.getProperty("maven.surefire.debug") == null ? 180 : 0);

    private PluginManager pluginManager = null;

    private boolean origDefaultUseCache = true;

    private static final Charset UTF8 = Charset.forName("UTF-8");

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

        env = new TestEnvironment(testDescription);
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
            jenkins = newHudson();
            // If the initialization graph is corrupted, we cannot expect that Jenkins is in the good shape.
            // Likely it is an issue in @Initializer() definitions (see JENKINS-37759).
            // So we just fail the test.
            if (jenkins.getInitLevel() != InitMilestone.COMPLETED) {
                throw new Exception("Jenkins initialization has not reached the COMPLETED initialization stage. Current state is " + jenkins.getInitLevel() +
                        ". Likely there is an issue with the Initialization task graph (e.g. usage of @Initializer(after = InitMilestone.COMPLETED)). See JENKINS-37759 for more info");
            }
        } catch (Exception e) {
            // if Hudson instance fails to initialize, it leaves the instance field non-empty and break all the rest of the tests, so clean that up.
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

    /**
     * Override to tear down your specific external resource.
     */
    public void after() throws Exception {
        try {
            if (jenkins!=null) {
                for (EndOfTestListener tl : jenkins.getExtensionList(EndOfTestListener.class))
                    tl.onTearDown();
            }
        } finally {
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

            // Hudson creates ClassLoaders for plugins that hold on to file descriptors of its jar files,
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
    }

    private static void jettyLevel(Level level) {
        Logger.getLogger("org.eclipse.jetty").setLevel(level);
    }

    /**
     * Backward compatibility with JUnit 4.8.
     */
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return apply(base,Description.createTestDescription(method.getMethod().getDeclaringClass(), method.getName(), method.getAnnotations()));
    }

    public Statement apply(final Statement base, final Description description) {

        Statement wrapped = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testDescription = description;
                Thread t = Thread.currentThread();
                String o = t.getName();
                t.setName("Executing "+ testDescription.getDisplayName());
                System.out.println("=== Starting " + testDescription.getDisplayName());
                before();
                try {
                    // so that test code has all the access to the system
                    ACL.impersonate(ACL.SYSTEM);
                    try {
                        base.evaluate();
                    } catch (Throwable th) {
                        // allow the late attachment of a debugger in case of a failure. Useful
                        // for diagnosing a rare failure
                        try {
                            throw new BreakException();
                        } catch (BreakException e) {}

                        throw th;
                    }
                } finally {
                    after();
                    testDescription = null;
                    t.setName(o);
                }
            }
        };

        // TODO: Support passing timeout as a Jenkinsfile Runner arg
        // testTimeout = getTestTimeoutOverride(description);
        final int testTimeout = 0;
        if (testTimeout <= 0) {
            System.out.println("Test timeout disabled.");
            return wrapped;
        } else {
            final Statement timeoutStatement = Timeout.seconds(testTimeout).apply(wrapped, description);
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        timeoutStatement.evaluate();
                    } catch (TestTimedOutException x) {
                        // withLookingForStuckThread does not work well; better to just have a full thread dump.
                        LOGGER.warning(String.format("Test timed out (after %d seconds).", testTimeout));
                        dumpThreads();
                        throw x;
                    }
                }
            };
        }
    }

    @SuppressWarnings("serial")
    public static class BreakException extends Exception {}

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
    protected Hudson newHudson() throws Exception {
        jettyLevel(Level.WARNING);
        ServletContext webServer = createWebServer();
        File home = homeLoader.allocate();
        for (JenkinsRecipe.Runner r : recipes) {
            r.decorateHome(this, home);
        }
        try {
            return new Hudson(home, webServer, getPluginManager());
        } catch (InterruptedException x) {
            throw new AssumptionViolatedException("Jenkins startup interrupted", x);
        } finally {
            jettyLevel(Level.INFO);
        }
    }

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
     *      null to let Jenkins create a new instance of default plugin manager, like it normally does when running as a webapp outside the test.
     */
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        if (jenkins!=null)
            throw new IllegalStateException("Too late to override the plugin manager");
    }

    public JenkinsRule with(PluginManager pluginManager) {
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

    /**
     * Configures a security realm for a test.
     */
    protected LoginService configureUserRealm() {
        HashLoginService realm = new HashLoginService();
        realm.setName("default");   // this is the magic realm name to make it effective on everywhere
        UserStore userStore = new UserStore();
        realm.setUserStore( userStore );
        userStore.addUser("alice", new Password("alice"), new String[]{"user","female"});
        userStore.addUser("bob", new Password("bob"), new String[]{"user","male"});
        userStore.addUser("charlie", new Password("charlie"), new String[]{"user","male"});

        return realm;
    }

//
// Convenience methods
//

    /**
     * Creates a new job.
     *
     * @param type Top level item type.
     * @param name Item name.
     *
     * @throws IllegalArgumentException if the project of the given name already exists.
     */
    public <T extends TopLevelItem> T createProject(Class<T> type, String name) throws IOException {
        return jenkins.createProject(type, name);
    }

    /**
     * Creates a new job with an unique name.
     *
     * @param type Top level item type.
     */
    public <T extends TopLevelItem> T createProject(Class<T> type) throws IOException {
        return jenkins.createProject(type, createUniqueProjectName());
    }

    /**
     * Creates a simple folder that other jobs can be placed in.
     * @since 1.494
     */
    public MockFolder createFolder(String name) throws IOException {
        return createProject(MockFolder.class, name);
    }

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
     * This is useful during debugging a test so that one can inspect the state of Hudson through the web browser.
     */
    public void interactiveBreak() throws Exception {
        System.out.println("Jenkins is running at " + getURL());
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Pauses the execution until ENTER is hit in the console.
     * <p>
     * This is often very useful so that you can interact with Hudson
     * from an browser, while developing a test case.
     */
    public void pause() throws IOException {
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Asserts that the outcome of the build is a specific outcome.
     */
    public <R extends Run> R assertBuildStatus(Result status, R r) throws Exception {
        if(status==r.getResult())
            return r;

        // dump the build output in failure message (in case BuildWatcher is not being used)
        String msg = "unexpected build status; build log was:\n------\n" + getLog(r) + "\n------\n";
        assertThat(msg, r.getResult(), is(status));
        return r;
    }

    public <R extends Run> R assertBuildStatus(Result status, Future<? extends R> r) throws Exception {
        assertThat("build was actually scheduled", r, notNullValue());
        return assertBuildStatus(status, r.get());
    }

    public <R extends Run> R assertBuildStatusSuccess(R r) throws Exception {
        assertBuildStatus(Result.SUCCESS, r);
        return r;
    }

    public <R extends Run> R assertBuildStatusSuccess(Future<? extends R> r) throws Exception {
        return assertBuildStatus(Result.SUCCESS, r);
    }

    public <J extends Job<J,R>,R extends Run<J,R>> R buildAndAssertSuccess(final J job) throws Exception {
        assertThat(job, IsInstanceOf.instanceOf(ParameterizedJobMixIn.ParameterizedJob.class));
        QueueTaskFuture f = new ParameterizedJobMixIn() {
            @Override protected Job asJob() {
                return job;
            }
        }.scheduleBuild2(0);
        @SuppressWarnings("unchecked") // no way to make this compile checked
        Future<R> f2 = f;
        return assertBuildStatusSuccess(f2);
    }

    /**
     * Avoids need for cumbersome {@code this.<J,R>buildAndAssertSuccess(...)} type hints under JDK 7 javac (and supposedly also IntelliJ).
     */
    public FreeStyleBuild buildAndAssertSuccess(FreeStyleProject job) throws Exception {
        return assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    /**
     * Asserts that the console output of the build contains the given substring.
     */
    public void assertLogContains(String substring, Run run) throws IOException {
        assertThat(getLog(run), containsString(substring));
    }

    /**
     * Asserts that the console output of the build does not contain the given substring.
     */
    public void assertLogNotContains(String substring, Run run) throws IOException {
        assertThat(getLog(run), not(containsString(substring)));
    }

    /**
     * Get entire log file as plain text.
     * {@link Run#getLog()} is deprecated for reasons that are irrelevant in tests,
     * and also does not strip console annotations which are a distraction in test output.
     */
    public static String getLog(Run run) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            run.getLogText().writeLogTo(0, baos);
        } catch (FileNotFoundException x) {
            return ""; // log file not yet created, OK
        }
        return baos.toString(run.getCharset().name());
    }

    /**
     * Waits for a build to complete.
     * @return the same build, once done
     * @since 1.607
     */
    public <R extends Run<?,?>> R waitForCompletion(R r) throws InterruptedException {
        // Could be using com.jayway.awaitility:awaitility but it seems like overkill here.
        while (r.isBuilding()) {
            Thread.sleep(100);
        }
        return r;
    }

    /**
     * Waits for a build log to contain a specified string.
     * @return the same build, once it does
     * @since 1.607
     */
    public <R extends Run<?,?>> R waitForMessage(String message, R r) throws IOException, InterruptedException {
        while (!getLog(r).contains(message)) {
            if (!r.isLogUpdated()) {
                assertLogContains(message, r); // should now fail
            }
            Thread.sleep(100);
        }
        return r;
    }

    public void assertStringContains(String message, String haystack, String needle) {
        assertThat(message, haystack, containsString(needle));
    }

    public void assertStringContains(String haystack, String needle) {
        assertThat(haystack, containsString(needle));
    }

    /**
     * Tokenizes "foo,bar,zot,-bar" and returns "foo,zot" (the token that starts with '-' is handled as
     * a cancellation.
     */
    private List<String> listProperties(String properties) {
        List<String> props = new ArrayList<String>(Arrays.asList(properties.split(",")));
        for (String p : props.toArray(new String[props.size()])) {
            if (p.startsWith("-")) {
                props.remove(p);
                props.remove(p.substring(1));
            }
        }
        return props;
    }

    /**
     * Creates a {@link TaskListener} connected to stdout.
     */
    public TaskListener createTaskListener() {
        return new StreamTaskListener(new CloseProofOutputStream(System.out));
    }

    /**
     * Asserts that two JavaBeans are equal as far as the given list of properties are concerned.
     *
     * <p>
     * This method takes two objects that have properties (getXyz, isXyz, or just the public xyz field),
     * and makes sure that the property values for each given property are equals (by using {@link org.junit.Assert#assertThat(Object, org.hamcrest.Matcher)})
     *
     * <p>
     * Property values can be null on both objects, and that is OK, but passing in a property that doesn't
     * exist will fail an assertion.
     *
     * <p>
     * This method is very convenient for comparing a large number of properties on two objects,
     * for example to verify that the configuration is identical after a config screen roundtrip.
     *
     * @param lhs
     *      One of the two objects to be compared.
     * @param rhs
     *      The other object to be compared
     * @param properties
     *      ','-separated list of property names that are compared.
     * @since 1.297
     */
    public void assertEqualBeans(Object lhs, Object rhs, String properties) throws Exception {
        assertThat("LHS", lhs, notNullValue());
        assertThat("RHS", rhs, notNullValue());
        for (String p : properties.split(",")) {
            PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(lhs, p);
            Object lp,rp;
            if(pd==null) {
                // field?
                try {
                    Field f = lhs.getClass().getField(p);
                    lp = f.get(lhs);
                    rp = f.get(rhs);
                } catch (NoSuchFieldException e) {
                    assertThat("No such property " + p + " on " + lhs.getClass(), pd, notNullValue());
                    return;
                }
            } else {
                lp = PropertyUtils.getProperty(lhs, p);
                rp = PropertyUtils.getProperty(rhs, p);
            }

            if (lp!=null && rp!=null && lp.getClass().isArray() && rp.getClass().isArray()) {
                // deep array equality comparison
                int m = Array.getLength(lp);
                int n = Array.getLength(rp);
                assertThat("Array length is different for property " + p, n, is(m));
                for (int i=0; i<m; i++)
                    assertThat(p + "[" + i + "] is different", Array.get(rp, i), is(Array.get(lp,i)));
                return;
            }

            assertThat("Property " + p + " is different", rp, is(lp));
        }
    }

    public void setQuietPeriod(int qp) throws IOException {
        jenkins.setQuietPeriod(qp);
    }

    /**
     * Works like {@link #assertEqualBeans(Object, Object, String)} but figure out the properties
     * via {@link org.kohsuke.stapler.DataBoundConstructor} and {@link org.kohsuke.stapler.DataBoundSetter}
     */
    public void assertEqualDataBoundBeans(Object lhs, Object rhs) throws Exception {
        if (lhs==null && rhs==null)     return;
        if (lhs==null)      fail("lhs is null while rhs="+rhs);
        if (rhs==null)      fail("rhs is null while lhs="+lhs);

        Constructor<?> lc = findDataBoundConstructor(lhs.getClass());
        Constructor<?> rc = findDataBoundConstructor(rhs.getClass());
        assertThat("Data bound constructor mismatch. Different type?", (Constructor)rc, is((Constructor)lc));

        String[] names = ClassDescriptor.loadParameterNames(lc);
        Class<?>[] types = lc.getParameterTypes();
        assertThat(types.length, is(names.length));
        assertEqualProperties(lhs, rhs, names, types);

        Map<String, Class<?>> lprops = extractDataBoundSetterProperties(lhs.getClass());
        Map<String, Class<?>> rprops = extractDataBoundSetterProperties(rhs.getClass());
        assertThat("Data bound setters mismatch. Different type?", lprops, is(rprops));
        List<String> setterNames = new ArrayList<String>();
        List<Class<?>> setterTypes = new ArrayList<Class<?>>();
        for (Map.Entry<String, Class<?>> e : lprops.entrySet()) {
            setterNames.add(e.getKey());
            setterTypes.add(e.getValue());
        }
        assertEqualProperties(lhs, rhs, setterNames.toArray(new String[0]), setterTypes.toArray(new Class<?>[0]));
    }

    private void assertEqualProperties(@Nonnull Object lhs, @Nonnull Object rhs, @Nonnull String[] names, @Nonnull Class<?>[] types) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, Exception {
        List<String> primitiveProperties = new ArrayList<String>();

        for (int i=0; i<types.length; i++) {
            Object lv = ReflectionUtils.getPublicProperty(lhs, names[i]);
            Object rv = ReflectionUtils.getPublicProperty(rhs, names[i]);

            if (lv != null && rv != null && Iterable.class.isAssignableFrom(types[i])) {
                Iterable lcol = (Iterable) lv;
                Iterable rcol = (Iterable) rv;
                Iterator ltr,rtr;
                for (ltr=lcol.iterator(), rtr=rcol.iterator(); ltr.hasNext() && rtr.hasNext();) {
                    Object litem = ltr.next();
                    Object ritem = rtr.next();

                    if (findDataBoundConstructor(litem.getClass())!=null) {
                        assertEqualDataBoundBeans(litem,ritem);
                    } else {
                        assertThat(ritem, is(litem));
                    }
                }
                assertThat("collection size mismatch between " + lhs + " and " + rhs, ltr.hasNext() ^ rtr.hasNext(),
                        is(false));
            } else
            if (findDataBoundConstructor(types[i])!=null || (lv!=null && findDataBoundConstructor(lv.getClass())!=null) || (rv!=null && findDataBoundConstructor(rv.getClass())!=null)) {
                // recurse into nested databound objects
                assertEqualDataBoundBeans(lv,rv);
            } else {
                primitiveProperties.add(names[i]);
            }
        }

        // compare shallow primitive properties
        if (!primitiveProperties.isEmpty())
            assertEqualBeans(lhs,rhs,Util.join(primitiveProperties,","));
    }

    @Nonnull
    private Map<String, Class<?>> extractDataBoundSetterProperties(@Nonnull Class<?> c) {
        Map<String, Class<?>> ret = new HashMap<String, Class<?>>();
        for ( ;c != null; c = c.getSuperclass()) {

            for (Field f: c.getDeclaredFields()) {
                if (f.getAnnotation(DataBoundSetter.class) == null) {
                    continue;
                }
                f.setAccessible(true);
                ret.put(f.getName(), f.getType());
           }

            for (Method m: c.getDeclaredMethods()) {
                AbstractMap.SimpleEntry<String, Class<?>> nameAndType = extractDataBoundSetter(m);
                if (nameAndType == null) {
                    continue;
                }
                if (ret.containsKey(nameAndType.getKey())) {
                    continue;
                }
                ret.put(nameAndType.getKey(),  nameAndType.getValue());
            }
        }
        return ret;
    }

    @CheckForNull
    private AbstractMap.SimpleEntry<String, Class<?>> extractDataBoundSetter(@Nonnull Method m) {
        // See org.kohsuke.stapler.RequestImpl::findDataBoundSetter
        if (!Modifier.isPublic(m.getModifiers())) {
            return null;
        }
        if (!m.getName().startsWith("set")) {
            return null;
        }
        if (m.getParameterTypes().length != 1) {
            return null;
        }
        if (!m.isAnnotationPresent(DataBoundSetter.class)) {
            return null;
        }
        
        // setXyz -> xyz
        return new AbstractMap.SimpleEntry<String, Class<?>>(
                Introspector.decapitalize(m.getName().substring(3)),
                m.getParameterTypes()[0]
        );
    }

    /**
     * Makes sure that two collections are identical via {@link #assertEqualDataBoundBeans(Object, Object)}
     */
    public void assertEqualDataBoundBeans(List<?> lhs, List<?> rhs) throws Exception {
        assertThat(rhs.size(), is(lhs.size()));
        for (int i=0; i<lhs.size(); i++)
            assertEqualDataBoundBeans(lhs.get(i),rhs.get(i));
    }

    public Constructor<?> findDataBoundConstructor(Class<?> c) {
        for (Constructor<?> m : c.getConstructors()) {
            if (m.getAnnotation(DataBoundConstructor.class)!=null)
                return m;
        }
        return null;
    }

    /**
     * Gets the descriptor instance of the current Hudson by its type.
     */
    public <T extends Descriptor<?>> T get(Class<T> d) {
        return jenkins.getDescriptorByType(d);
    }


    /**
     * Returns true if Hudson is building something or going to build something.
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
     * Waits until Hudson finishes building everything, including those in the queue.
     */
    public void waitUntilNoActivity() throws Exception {
        waitUntilNoActivityUpTo(Integer.MAX_VALUE);
    }

    /**
     * Waits until Hudson finishes building everything, including those in the queue, or fail the test
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

    /**
     * Called during the {@link #before()} to give a test case an opportunity to
     * control the test environment in which Hudson is run.
     */
    public void recipe() throws Exception {
        // look for recipe meta-annotation
        try {
            for (final Annotation a : testDescription.getAnnotations()) {
                JenkinsRecipe r = a.annotationType().getAnnotation(JenkinsRecipe.class);
                if(r==null)     continue;
                final JenkinsRecipe.Runner runner = r.value().newInstance();
                recipes.add(runner);
                tearDowns.add(new LenientRunnable() {
                    public void run() throws Exception {
                        runner.tearDown(JenkinsRule.this,a);
                    }
                });
                runner.setup(this,a);
            }
        } catch (NoSuchMethodException e) {
            // not a plain JUnit test.
        }
    }

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
                                    throw new AssumptionViolatedException("copying dependencies was interrupted", x);
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

    public JenkinsRule withNewHome() {
        return with(HudsonHomeLoader.NEW);
    }

    public JenkinsRule withExistingHome(File source) throws Exception {
        return with(new HudsonHomeLoader.CopyExisting(source));
    }

    /**
     * Declares that this test case expects to start with one of the preset data sets.
     * See {@code test/src/main/preset-data/}
     * for available datasets and what they mean.
     */
    public JenkinsRule withPresetData(String name) {
        name = "/" + name + ".zip";
        URL res = getClass().getResource(name);
        if(res==null)   throw new IllegalArgumentException("No such data set found: "+name);

        return with(new HudsonHomeLoader.CopyExisting(res));
    }

    public JenkinsRule with(HudsonHomeLoader homeLoader) {
        this.homeLoader = homeLoader;
        return this;
    }

    /**
     * Sometimes a part of a test case may ends up creeping into the serialization tree of {@link hudson.model.Saveable#save()},
     * so detect that and flag that as an error.
     */
    private Object writeReplace() {
        throw new AssertionError("JenkinsRule " + testDescription.getDisplayName() + " is not supposed to be serialized");
    }

    // needs to keep reference, or it gets GC-ed.
    private static final Logger SPRING_LOGGER = Logger.getLogger("org.springframework");
    private static final Logger JETTY_LOGGER = Logger.getLogger("org.mortbay.log");

    static {
        // screen scraping relies on locale being fixed.
        Locale.setDefault(Locale.ENGLISH);

        {// enable debug assistance, since tests are often run from IDE
            Dispatcher.TRACE = true;
            MetaClass.NO_CACHE=true;
            // load resources from the source dir.
            File dir = new File("src/main/resources");
            if(dir.exists() && MetaClassLoader.debugLoader==null)
                try {
                    MetaClassLoader.debugLoader = new MetaClassLoader(
                        new URLClassLoader(new URL[]{dir.toURI().toURL()}));
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
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

    private static final Logger LOGGER = Logger.getLogger(JenkinsRule.class.getName());

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
        // this also prevents tests from falsely advertising Hudson
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

    public static class TestBuildWrapper extends BuildWrapper {
        public Result buildResultInTearDown;

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new BuildWrapper.Environment() {
                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    buildResultInTearDown = build.getResult();
                    return true;
                }
            };
        }

        @Extension
        public static class TestBuildWrapperDescriptor extends BuildWrapperDescriptor {
            @Override
            public boolean isApplicable(AbstractProject<?, ?> project) {
                return true;
            }

            @Override
            public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getDisplayName() {
                return "TestBuildWrapper";
            }
        }
    }

    public Description getTestDescription() {
        return testDescription;
    }
}
