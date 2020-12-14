package io.jenkins.jenkinsfile.runner.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;

/**
 * @author Kohsuke Kawaguchi
 */
public class ClassLoaderBuilder {
    private final ClassLoader parent;
    private final HashSet<URL> jars = new HashSet<>(48);

    public ClassLoaderBuilder(ClassLoader parent) {
        this.parent = parent;
    }

    /**
     * Recursively scan a directory to find jars
     */
    public ClassLoaderBuilder processJars(File dirOrFile, FileFilter filter, JarHandler handler) throws IOException {
        if (dirOrFile.isFile() && dirOrFile.getName().endsWith(".jar") && filter.accept(dirOrFile) ) {
            handler.accept(dirOrFile);
            return this;
        }

        File[] children = dirOrFile.listFiles();
        if (children!=null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    processJars(child, filter, handler);
                } else {
                    if (child.getName().endsWith(".jar") && filter.accept(child)) {
                        handler.accept(child);
                    }
                }
            }
        }
        return this;
    }

    public ClassLoaderBuilder collectJars(File dir) throws IOException {
        return processJars(dir, (File)->true, (jar) -> jars.add(jar.toURI().toURL()));
    }

    public ClassLoaderBuilder excludeJars(File dir) throws IOException {
        return processJars(dir, (File)->true, (jar) -> jars.remove(jar.toURI().toURL()));
    }

    public ClassLoader make() {
        return AccessController.doPrivileged((PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(jars.toArray(
                new URL[jars.size()]), parent));
    }

    public interface JarHandler {
        public void accept(File jar) throws MalformedURLException;
    }
}
