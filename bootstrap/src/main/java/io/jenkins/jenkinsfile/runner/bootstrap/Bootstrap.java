package io.jenkins.jenkinsfile.runner.bootstrap;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.util.VersionNumber;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main entry point for the Jenkinsfile Runner execution.
 *
 * @author Kohsuke Kawaguchi
 * @author Oleg Nenashev
 */
@Command(name = "jenkinsfile-runner", versionProvider = Bootstrap.VersionProviderImpl.class, sortOptions = false, mixinStandardHelpOptions = true)
public class Bootstrap implements Callable<Integer> {

    public static final long CACHE_EXPIRE = System.currentTimeMillis() - 24 * 3600 * 1000;
    private static final String WORKSPACES_DIR_SYSTEM_PROPERTY = "jenkins.model.Jenkins.workspacesDir";
    private static final String DEFAULT_JOBNAME = "job";
    @CheckForNull
    private static Properties JFR_PROPERTIES = null;

    /**
     * Exploded jenkins.war
     */
    @Option(names = { "-w", "--jenkins-war" },
            description = "Path to exploded jenkins war directory." +
                    "Depending on packaging, it may contain the entire WAR " +
                    "or just resources to be loaded by the WAR file, for example Groovy hooks or extra libraries.")
    @CheckForNull
    public File warDir;

    /**
     * Where to load plugins from?
     */
    @Option(names = { "-p", "--plugins" },
            description = "Plugins required to run pipeline. Either a plugins.txt file or a /plugins installation directory. Defaults to plugins.txt")
    public File pluginsDir;

    /**
     * Checked out copy of the working space.
     */
    @Option(names = { "-f", "--file" },
            description = "Path to Jenkinsfile or directory containing a Jenkinsfile, defaults to ./Jenkinsfile")
    public File jenkinsfile;

    /**
     * Workspace for the Run
     */
    @CheckForNull
    @Option(names = "--runWorkspace",
            description = "Path to the workspace of the run to be used within the node{} context. " +
                    "It applies to both Jenkins master and agents (or side containers) if any. " +
                    "Requires Jenkins 2.119 or above")
    public File runWorkspace;

    @Option(names = { "-jv", "--jenkins-version"},
            description = "Jenkins version to use if Jenkins WAR is not specified by --jenkins-war. Defaults to the latest LTS")
    @CheckForNull
    public String version;

    @Option(names = { "-m", "--mirror"},
            description = "mirror site of Jenkins, defaults to http://updates.jenkins.io/download. Get the mirror list from http://mirrors.jenkins-ci.org/status.html")
    public String mirror;

    /**
     * Jenkins Home for the Run.
     */
    @CheckForNull
    @Option(names = "--runHome",
            description = "Path to the empty Jenkins Home directory to use for this run. If not specified a temporary directory will be created. " +
            "Note that the folder specified via --runHome will not be disposed after the run")
    public File runHome;

    @CheckForNull
    @Option(names = "--withInitHooks",
            description = "Path to a directory containing Groovy Init Hooks to copy into init.groovy.d")
    public File withInitHooks;

    /**
     * Job name for the Run.
     */
    @Option(names = { "-n", "--job-name"},
            description = "Name of the job the run belongs to")
    public String jobName = DEFAULT_JOBNAME;

    /**
     * Cause of the Run.
     */
    @Option(names = { "-c", "--cause"},
            description = "Cause of the run")
    public String cause;

    /**
     * BuildNumber of the Run.
     */
    @Option(names = { "-b", "--build-number"},
            description = "Build number of the run")
    public int buildNumber = 1;

    @Option(names = { "-a", "--arg" },
            description = "Parameters to be passed to the build. Use multiple -a switches for multiple params")
    @CheckForNull
    public Map<String,String> workflowParameters;

    @Option(names = { "-ns", "--no-sandbox" },
            description = "Disable workflow job execution within sandbox environment")
    public boolean noSandBox;

    @Option(names = { "-u", "--keep-undefined-parameters" },
            description = "Keep undefined parameters if set")
    public boolean keepUndefinedParameters = false;

    @Option(names = "--cli",
            description = "Launch interactive CLI") //, forbids = { "-v", "--runWorkspace", "-a", "-ns" })
    public boolean cliOnly;

    @Option(names = "--scm",
            description = "YAML definition of the SCM, with optional credentials, to use for the project")
    public File scm;

    public static void main(String[] args) throws Throwable {
        // break for attaching profiler
        if (Boolean.getBoolean("start.pause")) {
            System.console().readLine();
        }

        int exitCode = new CommandLine(new Bootstrap()).execute(args);
        System.exit(exitCode);
    }

    @Override
    @SuppressFBWarnings("DM_EXIT")
    public Integer call() {
        try {
            postConstruct();
            return runJenkinsfile();
        } catch (Throwable ex) {
            throw new RuntimeException("Unhandled exception", ex);
        }
    }

    /**
     * Directory used as cache for downloaded artifacts.
     */
    private File cache = new File(System.getProperty("user.home") + "/.jenkinsfile-runner/");

    @PostConstruct
    public void postConstruct() throws IOException {

        if (System.getenv("FORCE_JENKINS_CLI") != null) {
            this.cliOnly = true;
        }

        // Process the Jenkins version
        if (this.version != null && this.warDir != null) {
            System.err.printf("Error: --jenkins-war and --jenkins-version are mutually exclusive options");
            System.exit(-1);
        }
        if (this.version != null && !isVersionSupported()) {
            System.err.printf("Jenkins version [%s] not supported by this jenkinsfile-runner version (requires %s). %n",
                    this.version,
                    getMininumJenkinsVersion());
            System.exit(-1);
        }
        if (warDir == null) {
            warDir= getJenkinsWar();
        }

        if (this.jenkinsfile == null) this.jenkinsfile = new File("Jenkinsfile");
        if (!this.cliOnly && !this.jenkinsfile.exists()) {
            System.err.println("no Jenkinsfile in current directory.");
            System.exit(-1);
        }
        if (this.jenkinsfile.isDirectory()) this.jenkinsfile = new File(this.jenkinsfile, "Jenkinsfile");

        if (this.pluginsDir == null) {
            this.pluginsDir = new File("plugins.txt");
        } else if (!this.pluginsDir.exists()) {
            System.err.println("invalid plugins file.");
            System.exit(-1);
        }

        if (this.pluginsDir.isFile()) {

            File plugins_txt = this.pluginsDir;
            // This is a plugin list file
            this.pluginsDir = Files.createTempDirectory("plugins").toFile();
            for (String line : FileUtils.readLines(plugins_txt, UTF_8)) {
                String shortname = line;
                String version = "latest";

                int i = line.indexOf(':');
                if (i != -1) {
                    shortname = line.substring(0,i);
                    version = line.substring(i+1);
                }

                installPlugin(shortname, version);
            }
        }

        if (this.runWorkspace != null){
            if (System.getProperty(WORKSPACES_DIR_SYSTEM_PROPERTY) != null) {
                //TODO(oleg_nenashev): It would have been better to keep it other way, but made it as is to retain compatibility
                System.out.println("Ignoring the --runWorkspace argument, because an explicit System property is set (-D" + WORKSPACES_DIR_SYSTEM_PROPERTY + ")");
            } else {
                System.setProperty(WORKSPACES_DIR_SYSTEM_PROPERTY, this.runWorkspace.getAbsolutePath());
            }
        }

        if (this.workflowParameters == null){
            this.workflowParameters = new HashMap<>();
        }
        for (Map.Entry<String, String> workflowParameter : this.workflowParameters.entrySet()) {
            if (workflowParameter.getValue() == null) {
                workflowParameter.setValue("");
            }
        }
        if (this.cause != null) {
           this.cause = this.cause.trim();
           if (this.cause.isEmpty()) this.cause = null;
        }

        if (this.jobName.isEmpty()) this.jobName = DEFAULT_JOBNAME;

        if (this.keepUndefinedParameters) {
          System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");
        }
    }

    static class VersionProviderImpl implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            return new String[] { readJenkinsPomProperty("jfr.version") };
        }
    }

    private static String getMininumJenkinsVersion() throws IOException {
        return readJenkinsPomProperty("minimum.jenkins.version");
    }

    private boolean isVersionSupported() throws IOException {
        return new VersionNumber(this.version).isNewerThanOrEqualTo(new VersionNumber(getMininumJenkinsVersion()));
    }

    private static String readJenkinsPomProperty(String key) throws IOException {
        if (JFR_PROPERTIES != null) {
            return JFR_PROPERTIES.getProperty(key);
        }
        try (InputStream pomProperties = Bootstrap.class.getResourceAsStream("/jfr.properties")) {
            Properties props = new Properties();
            props.load(pomProperties);
            JFR_PROPERTIES = props;
            return props.getProperty(key);
        }
    }

    private File getJenkinsWar() throws IOException {
        if (version == null) {
            System.out.println("No explicit version has been selected, using latest LTS");

            File latestCore = new File(cache, "war/latest.txt");
            latestCore.getParentFile().mkdirs();
            // Check once a day
            if (!latestCore.exists() || latestCore.lastModified() < CACHE_EXPIRE) {
                FileUtils.copyURLToFile(URI.create("http://updates.jenkins.io/stable/latestCore.txt").toURL(), latestCore);
            }
            version = FileUtils.readFileToString(latestCore, StandardCharsets.US_ASCII);
        }

        System.out.printf("Running pipeline on jenkins %s%n", version);

        File war = new File(cache, String.format("war/%s/jenkins-war-%s.war", version, version));
        if (!war.exists()) {
            war.getParentFile().mkdirs();
            final URL url = new URL(getMirrorURL(String.format("http://updates.jenkins.io/download/war/%s/jenkins.war", version)));
            System.out.printf("Downloading jenkins %s...%n", version);
            FileUtils.copyURLToFile(url, war);
        }

        return war;
    }

    private void installPlugin(String shortname, String version) throws IOException {

        final File install = new File(pluginsDir, shortname + ".jpi");
        File plugin = new File(cache, String.format("plugins/%s/%s-%s.hpi", shortname, shortname, version));
        if (!plugin.exists() || ("latest".equals(version) && plugin.lastModified() < CACHE_EXPIRE) ) {
            plugin.getParentFile().mkdirs();
            final URL url = new URL(getMirrorURL(String.format("https://updates.jenkins.io/download/plugins/%s/%s/%s.hpi", shortname, version, shortname)));
            System.out.printf("Downloading jenkins plugin %s (%s)...%n", shortname, version);
            FileUtils.copyURLToFile(url, plugin);
        }

        Files.createSymbolicLink(install.toPath(), plugin.toPath());
    }

    private String getMirrorURL(String url) {
        if (this.mirror == null || "".equals(this.mirror.trim())) {
            return url;
        }

        return url.replace("https://updates.jenkins.io/download", this.mirror);
    }

    public int runJenkinsfile() throws Throwable {
        String appClassName = "io.jenkins.jenkinsfile.runner.App";
        if (hasClass(appClassName)) {
            Class<?> c = Class.forName(appClassName);
            return  ((IApp) c.newInstance()).run(this);
        }

        // Explode war if necessary
        String warPath = warDir.getAbsolutePath();
        if(FilenameUtils.getExtension(warPath).equals("war") && new File(warPath).isFile()) {
            System.out.println("Exploding," + warPath +  "this might take some time.");
            warDir = Util.explodeWar(warPath);
        }

        ClassLoader jenkins = createJenkinsWarClassLoader();
        ClassLoader setup = createSetupClassLoader(jenkins);

        Thread.currentThread().setContextClassLoader(setup);    // or should this be 'jenkins'?

        try {
            Class<?> c = setup.loadClass(appClassName);
            return ((IApp) c.newInstance()).run(this);
        } catch (ClassNotFoundException e) {
            if (setup instanceof URLClassLoader) {
                throw new ClassNotFoundException(e.getMessage() + " not found in " + getAppRepo() + ","
                        + new File(warDir, "WEB-INF/lib") + " " + Arrays.toString(((URLClassLoader) setup).getURLs()),
                        e);
            } else {
                throw e;
            }
        }
    }

    public ClassLoader createJenkinsWarClassLoader() throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<ClassLoader>) () -> new ClassLoaderBuilder(new SideClassLoader(getPlatformClassloader()))
                .collectJars(new File(warDir, "WEB-INF/lib"))
                // servlet API needs to be visible to jenkins.war
                .collectJars(new File(getAppRepo(), "javax/servlet"))
                .make());
    }

    public ClassLoader createSetupClassLoader(ClassLoader jenkins) throws IOException {
        return new ClassLoaderBuilder(jenkins)
                .collectJars(getAppRepo())
                .make();
    }

    /**
     * In JDK 11, platform classes are not accessible by default but through the platform classloader.
     */
    private ClassLoader getPlatformClassloader() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (isPostJava8()) {
            return (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        }

        return null;
    }

    private static boolean isPostJava8() {
        String javaVersion = System.getProperty("java.version");
        return !javaVersion.startsWith("1.");
    }

    public boolean hasClass(String className) {
        try  {
            Class.forName(className);
            return true;
        }  catch (ClassNotFoundException e) {
            return false;
        }
    }

    public File getAppRepo() {
        return new File(System.getProperty("app.repo"));
    }
}
