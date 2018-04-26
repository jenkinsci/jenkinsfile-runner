package io.jenkins.jenkinsfile.runner.bootstrap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Kohsuke Kawaguchi
 */
public class Bootstrap {

    public static final long CACHE_EXPIRE = System.currentTimeMillis() - 24 * 3600 * 1000;
    /**
     * This system property is set by the bootstrap script created by appassembler Maven plugin
     * to point to a local Maven repository.
     */
    public final File appRepo = new File(System.getProperty("app.repo"));

    /**
     * Exploded jenkins.war
     */
    @Option(name = "-w", aliases = { "--jenkins-war" }, usage = "path to jenkins.war or exploded jenkins war directory", forbids = { "-v" })
    public File warDir;

    @Option(name = "-v", aliases = { "--version"}, usage = "jenkins version to use. Defaults to latest LTS.")
    public String version;
    /**
     * Where to load plugins from?
     */
    @Option(name = "-p", aliases = { "--plugins" }, usage = "plugins required to run pipeline. Either a plugins.txt file or a /plugins installation directory. Defaults to plugins.txt.")
    public File pluginsDir;

    /**
     * Checked out copy of the working space.
     */
    @Option(name = "-f", aliases = { "--file" }, usage = "Path to Jenkinsfile (or directory containing a Jenkinsfile) to run, default to ./Jenkinsfile.")
    public File jenkinsfile;


    public static void main(String[] args) throws Throwable {
        // break for attaching profiler
        if (Boolean.getBoolean("start.pause")) {
            System.console().readLine();
        }

        final Bootstrap bootstrap = new Bootstrap();
        CmdLineParser parser = new CmdLineParser(bootstrap);
        try {
            parser.parseArgument(args);
            bootstrap.postConstruct();
            final int status = bootstrap.run();
            System.exit(status);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    /**
     * Directory used as cache for downloaded artifacts.
     */
    private File cache = new File(System.getProperty("user.home") + "/.jenkinsfile-runner/");

    @PostConstruct
    private void postConstruct() throws IOException {

        if (warDir == null) {
            warDir= getJenkinsWar();
        }

        if (this.jenkinsfile == null) this.jenkinsfile = new File("Jenkinsfile");
        if (!this.jenkinsfile.exists()) {
            System.err.println("no Jenkinsfile in current directory.");
            System.exit(-1);
        }
        if (this.jenkinsfile.isDirectory()) this.jenkinsfile = new File(this.jenkinsfile, "Jenkinsfile");

        if (this.pluginsDir == null) {
            this.pluginsDir = new File("plugins.txt");
        }

        if (!this.pluginsDir.exists()) {
            System.err.println("invalid plugins file.");
            System.exit(-1);
        }

        if (this.pluginsDir.isFile()) {

            File plugins_txt = this.pluginsDir;
            // This is a plugin list file
            this.pluginsDir = Files.createTempDirectory("plugins").toFile();
            for (String line : FileUtils.readLines(plugins_txt, UTF_8)) {
                int i = line.indexOf(':');
                String shortname = line.substring(0,i);
                String version = line.substring(i+1);
                installPlugin(shortname, version);
            }
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

        System.out.printf("Running pipeline on jenkins %s\n", version);

        File war = new File(cache, String.format("war/%s/jenkins-war-%s.war", version, version));
        if (!war.exists()) {
            war.getParentFile().mkdirs();
            final URL url = new URL(String.format("http://updates.jenkins.io/download/war/%s/jenkins.war", version));
            System.out.printf("Downloading jenkins %s...\n", version);
            FileUtils.copyURLToFile(url, war);
        }

        return war;
    }

    private void installPlugin(String shortname, String version) throws IOException {

        final File install = new File(pluginsDir, shortname + ".jpi");
        File plugin = new File(cache, String.format("plugins/%s/%s-%s.hpi", shortname, shortname, version));
        if (!plugin.exists() || ("latest".equals(version) && plugin.lastModified() < CACHE_EXPIRE) ) {
            plugin.getParentFile().mkdirs();
            final URL url = new URL(String.format("http://updates.jenkins.io/download/plugins/%s/%s/%s.hpi", shortname, version, shortname));
            System.out.printf("Downloading jenkins plugin %s (%s)...\n", shortname, version);
            FileUtils.copyURLToFile(url, plugin);
        }

        Files.createSymbolicLink(install.toPath(), plugin.toPath());
    }

    public int run() throws Throwable {
        ClassLoader jenkins = createJenkinsWarClassLoader();
        ClassLoader setup = createSetupClassLoader(jenkins);

        Thread.currentThread().setContextClassLoader(setup);    // or should this be 'jenkins'?

        Class<?> c = setup.loadClass("io.jenkins.jenkinsfile.runner.App");
        return ((IApp)c.newInstance()).run(this);
    }

    public ClassLoader createJenkinsWarClassLoader() throws IOException {
        return new ClassLoaderBuilder(new SideClassLoader(null))
                .collectJars(new File(warDir,"WEB-INF/lib"))
                // servlet API needs to be visible to jenkins.war
                .collectJars(new File(appRepo,"javax/servlet"))
                .make();
    }

    public ClassLoader createSetupClassLoader(ClassLoader jenkins) throws IOException {
        return new ClassLoaderBuilder(jenkins)
                .collectJars(appRepo)
                .make();
    }
}
