package io.jenkins.jenkinsfile.runner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

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
    public final File warDir;

    public final File pluginsDir;

    public Bootstrap(File warDir, File pluginsDir) {
        this.warDir = warDir;
        this.pluginsDir = pluginsDir;
    }

    public static void main(String[] args) throws Throwable {
        // TODO: support exploding war. See WebInfConfiguration.unpack()
        if (args.length<2) {
            System.err.println("Usage: jenkinsfilerunner <jenkins.war> <pluginsDir>");
            System.exit(1);
        }

        System.exit(new Bootstrap(new File(args[0]), new File(args[1])).run());
    }

    public int run() throws Throwable {
        ClassLoader jenkins = createJenkinsWarClassLoader();
        ClassLoader setup = createSetupClassLoader(jenkins);

        Class<?> c = setup.loadClass("io.jenkins.jenkinsfile.runner.App");
        return (int)c.getMethod("run",File.class,File.class).invoke(
                c.newInstance(), warDir, pluginsDir
        );
    }

    public ClassLoader createJenkinsWarClassLoader() throws IOException {
        List<URL> jars = collectJars(new File(warDir,"WEB-INF/lib"),(File f)->true, new ArrayList<>());
        // servlet API needs to be visible to jenkins.war
        collectJars(new File(appRepo,"javax/servlet"),(File f)->true, jars);

        return new URLClassLoader(jars.toArray(new URL[jars.size()]), null);
    }

    public ClassLoader createSetupClassLoader(ClassLoader jenkins) throws IOException {
        List<URL> jars = collectJars(
                appRepo,
                (File f)->true,
                new ArrayList<>());

        return new URLClassLoader(jars.toArray(new URL[jars.size()]), jenkins);
    }

    /**
     * Recursively scan a directory to find jars
     */
    private List<URL> collectJars(File dir, FileFilter filter, List<URL> jars) throws IOException {
        File[] children = dir.listFiles();
        if (children!=null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    collectJars(child, filter, jars);
                } else {
                    if (child.getName().endsWith(".jar") && filter.accept(child)) {
                        jars.add(child.toURI().toURL());
                    }
                }
            }
        }
        return jars;    // just to make this method flow a bit better
    }
}
