package io.jenkins.jenkinsfile.runner;

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

    @DataBoundConstructor
    public SCMContainer(SCM scm) {
        this.scm = scm;
    }

    public SCM getSCM() {
        return scm;
    }

    public static SCM loadFromYAML(File input) throws ConfiguratorException {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        YamlSource<Path> ys = YamlSource.of(input.toPath());

        Mapping config = YamlUtils.loadFrom(Collections.singletonList(ys), context);

        SCMContainer container = (SCMContainer) registry.lookupOrFail(SCMContainer.class).configure(config, context);

        return container.getSCM();
    }
}
