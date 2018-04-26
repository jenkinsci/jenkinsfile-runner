package io.jenkins.jenkinsfile.runner.bootstrap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Bootstrap {

    /**
     * This system property is set by the bootstrap script created by appassembler Maven plugin
     * to point to a local Maven repository.
     */
    public final File appRepo = new File(System.getProperty("app.repo"));

    /**
     * Exploded jenkins.war
     */
    @Option(name = "-w", aliases = { "--jenkins-war" }, usage = "path to jenkins.war or exploded jenkins war directory", required = true)
    public File warDir;

    /**
     * Where to load plugins from?
     */
    @Option(name = "-p", aliases = { "--plugins" }, usage = "plugins directory, default to ./plugins")
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

    @PostConstruct
    private void postConstruct() {
        if (this.jenkinsfile == null) this.jenkinsfile = new File("Jenkinsfile");
        if (this.jenkinsfile.isDirectory()) this.jenkinsfile = new File(this.jenkinsfile, "Jenkinsfile");

        if (this.pluginsDir == null) this.pluginsDir = new File("plugins");
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
