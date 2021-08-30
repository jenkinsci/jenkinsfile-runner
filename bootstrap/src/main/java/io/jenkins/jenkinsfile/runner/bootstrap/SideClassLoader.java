package io.jenkins.jenkinsfile.runner.bootstrap;

/**
 * Expose bootstrap classes into the rest of the system.
 *
 * @author Kohsuke Kawaguchi
 */
public class SideClassLoader extends ClassLoader {
    public SideClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("io.jenkins.jenkinsfile.runner.bootstrap."))
            return getClass().getClassLoader().loadClass(name);
        return super.findClass(name);
    }
}
