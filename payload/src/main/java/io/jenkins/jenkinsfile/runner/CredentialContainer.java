package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
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
    private final StandardUsernameCredentials creds;

    @DataBoundConstructor
    public CredentialContainer(StandardUsernameCredentials creds) {
        this.creds = creds;
    }

    public StandardUsernameCredentials getCreds() {
        return creds;
    }

    public static StandardUsernameCredentials loadFromYAML(File input) throws ConfiguratorException {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        YamlSource<Path> ys = YamlSource.of(input.toPath());

        Mapping config = YamlUtils.loadFrom(Collections.singletonList(ys), context);

        CredentialContainer container = (CredentialContainer) registry.lookupOrFail(CredentialContainer.class).configure(config, context);

        return container.getCreds();
    }
}
