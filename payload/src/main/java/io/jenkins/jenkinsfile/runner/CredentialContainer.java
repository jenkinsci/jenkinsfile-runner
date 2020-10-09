package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.Credentials;
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

public class CredentialContainer {
    private final Credentials credential;

    @DataBoundConstructor
    public CredentialContainer(Credentials credential) {
        this.credential = credential;
    }

    public Credentials getCredential() {
        return credential;
    }

    public static Credentials loadFromYAML(File input) throws ConfiguratorException {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        YamlSource<Path> ys = YamlSource.of(input.toPath());

        Mapping config = YamlUtils.loadFrom(Collections.singletonList(ys), context);

        CredentialContainer container = (CredentialContainer) registry.lookupOrFail(CredentialContainer.class).configure(config, context);

        return container.getCredential();
    }
}
