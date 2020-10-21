package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.Credentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.scm.SCM;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

public class SCMContainer {
    private final SCM scm;
    private final Credentials credential;

    @DataBoundConstructor
    public SCMContainer(SCM scm, Credentials credential) {
        this.scm = scm;
        this.credential = credential;
    }

    public SCM getSCM() {
        return scm;
    }

    @CheckForNull
    public Credentials getCredential() {
        return credential;
    }

    public static SCMContainer loadFromYAML(File input) throws ConfiguratorException {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        YamlSource<Path> ys = YamlSource.of(input.toPath());

        Mapping config = YamlUtils.loadFrom(Collections.singletonList(ys), context);

        return (SCMContainer) registry.lookupOrFail(SCMContainer.class).configure(config, context);
    }
}
