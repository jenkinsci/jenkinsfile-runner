package io.jenkins.jenkinsfile.runner.bootstrap;

/**
 * Joins two classloaders.
 * The parent classloader always has a priority.
 */
public class PairClassLoader extends ClassLoader {

    final ClassLoader parent;
    final ClassLoader secondary;

    public PairClassLoader(ClassLoader parent, ClassLoader secondary) {
        this.parent = parent;
        this.secondary = secondary;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassNotFoundException parentException = null;
        try {
            return parent.loadClass(name);
        } catch (ClassNotFoundException ex) {
            parentException = ex;
        }

        try {
            return secondary.loadClass(name);
        } catch (ClassNotFoundException ex) {
            ClassNotFoundException mergedException = new ClassNotFoundException(name, ex);
            mergedException.addSuppressed(parentException);
            throw mergedException;
        }
    }
}
