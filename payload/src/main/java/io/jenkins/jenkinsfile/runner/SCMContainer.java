package io.jenkins.jenkinsfile.runner;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.scm.SCM;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
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

    public void addCredentialToStore() throws IOException, CredentialsUnavailableException {
        try {
            addCredentials();
        } catch (IOException | CredentialsUnavailableException e) {
            System.err.printf("could not create credentials: %s%n", e.getMessage());
            throw e;
        }
    }

    private void addCredentials() throws IOException, CredentialsUnavailableException {
        CredentialsStore store = getStore();
        if (store == null) {
            throw new CredentialsUnavailableException("Credentials specified but could not find credentials store");
        }
        Domain globalDomain = Domain.global();
        store.addCredentials(globalDomain, credential);
    }

    private CredentialsStore getStore() {
        CredentialsStore store = null;
        for (CredentialsStore s : CredentialsProvider.lookupStores(Jenkins.get())) {
            if (s.getProvider() instanceof SystemCredentialsProvider.ProviderImpl) {
                store = s;
                break;
            }
        }
        return store;
    }
}
