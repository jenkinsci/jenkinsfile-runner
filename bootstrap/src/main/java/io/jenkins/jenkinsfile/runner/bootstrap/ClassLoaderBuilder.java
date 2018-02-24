package io.jenkins.jenkinsfile.runner.bootstrap;

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
public class ClassLoaderBuilder {
    private final ClassLoader parent;
    private final List<URL> jars = new ArrayList<>();

    public ClassLoaderBuilder(ClassLoader parent) {
        this.parent = parent;
    }

    /**
     * Recursively scan a directory to find jars
     */
    public ClassLoaderBuilder collectJars(File dir, FileFilter filter) throws IOException {
        File[] children = dir.listFiles();
        if (children!=null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    collectJars(child, filter);
                } else {
                    if (child.getName().endsWith(".jar") && filter.accept(child)) {
                        jars.add(child.toURI().toURL());
                    }
                }
            }
        }
        return this;
    }

    public ClassLoaderBuilder collectJars(File dir) throws IOException {
        return collectJars(dir,(File)->true);
    }

    public ClassLoader make() {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]),parent);
    }
}
