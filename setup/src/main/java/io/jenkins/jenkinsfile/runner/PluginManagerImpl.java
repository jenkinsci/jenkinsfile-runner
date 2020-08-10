package io.jenkins.jenkinsfile.runner;

import hudson.JFRPluginStrategy;
import hudson.PluginManager;
import hudson.PluginStrategy;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
class PluginManagerImpl extends PluginManager {
    public PluginManagerImpl(ServletContext context, File rootDir) {
        super(context, rootDir);
    }

    @Override
    protected Collection<String> loadBundledPlugins() throws Exception {
        return Collections.emptySet();
    }

    @Override
    protected PluginStrategy createPluginStrategy() {
        return new JFRPluginStrategy(this);
    }
}
