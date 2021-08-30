package io.jenkins.jenkinsfile.runner.bootstrap.util;

/**
 * Classloader, which might be added to the instance post-factum during the execution.
 * In Jenkinsfile Runner it is used to set the plugin classloader once the Jenkins instance is initialized.
 */
public class OptionalClassLoader extends ClassLoader {

    private final ClassLoader parent;
    private ClassLoader optional;

    public OptionalClassLoader(ClassLoader parent) {
        this.parent = parent;
    }

    public void set(ClassLoader optional) {
        this.optional = optional;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassNotFoundException parentException;
        try {
            return parent.loadClass(name);
        } catch (ClassNotFoundException ex) {
            parentException = ex;
        }

        ClassLoader _opt = optional;
        if (_opt != null) {
            try {
                return _opt.loadClass(name);
            } catch (ClassNotFoundException ex) {
                ex.addSuppressed(parentException);
                throw ex;
            }
        }

        throw  parentException;
    }
}