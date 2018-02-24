package io.jenkins.jenkinsfile.runner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Kohsuke Kawaguchi
 */
public class Bootstrap {

    /**
     * This system property is set by the bootstrap script created by appassembler Maven plugin
     * to point to a local Maven repository.
     */
    private File appRepo = new File(System.getProperty("app.repo"));

    public static void main(String[] args) throws Exception {
        System.exit(new Bootstrap().run(new File(args[0])));
    }

    public int run(File war) throws Exception {
        ClassLoader jenkins = createJenkinsWarClassLoader(war);
        ClassLoader setup = createSetupClassLoader(jenkins);

        Callable<Integer> r = (Callable<Integer>) setup.loadClass("io.jenkins.jenkinsfile.runner.App").newInstance();
        return r.call();
    }

    public ClassLoader createJenkinsWarClassLoader(File war) throws IOException {
        // TODO: support exploding war. See WebInfConfiguration.unpack()

        List<URL> jars = collectJars(new File(war,"WEB-INF/lib"),(File f)->true, new ArrayList<>());
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
