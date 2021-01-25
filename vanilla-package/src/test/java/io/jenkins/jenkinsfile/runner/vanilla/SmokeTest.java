package io.jenkins.jenkinsfile.runner.vanilla;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.PersonIdent;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SmokeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog();

    @Rule
    public final SystemErrRule systemErr = new SystemErrRule().enableLog();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(120);

    @Test
    public void helloWorld() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "echo 'Hello, world!'", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should be executed successfully", result, equalTo(0));
        assertThat(systemOut.getLog(), containsString("Hello, world!"));
    }

    @Test
    public void shouldFailWithWrongJenkinsfile() throws Throwable {
        File jenkinsfile = new File(tmp.getRoot(), "Jenkinsfile");

        int result = JFRTestUtil.run(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
        assertThat(systemOut.getLog(), containsString("does not exist"));
    }

    // TODO: uncomment once JFR can do something about timeouts internally
    @Test
    @Ignore
    public void shouldHangWhenPipelineHangs() throws Throwable {
        File jenkinsfile = new File(tmp.getRoot(), "Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "stage('Hang!') {\n" +
                "    node {\n" +
                "        while(true) {\n" +
                "            // it hangs\n" +
                "        }\n" +
                "    }\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.run(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
    }

    @Test
    public void pipelineExecutionFails() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "node {\n" +
                "    currentBuild.result = 'FAILED'\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
        assertThat(systemOut.getLog(), containsString("Finished: FAILURE"));
    }

    @Test
    public void pipelineExecutionUnstable() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "node {\n" +
                "    currentBuild.result = 'UNSTABLE'\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
        assertThat(systemOut.getLog(), containsString("Finished: UNSTABLE"));
    }

    @Test
    public void pipelineBadSyntax() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "pipeline {\n" +
                "    agent any\n" +
                "    parameters {\n" +
                "        string(name: 'param1', defaultValue: '', description: 'Greeting message')\n" +
                "        string(name: 'param2', defaultValue: '', description: '2nd parameter')\n" +
                "    }\n" +
                "    stages {\n" +
                "        stage('Build') {\n" +
                "            // Missing steps, then it fails\n" +
                "            echo 'Hello world!'\n" +
                "            echo \"Value for param1: ${params.param1}\"\n" +
                "            echo \"Value for param2: ${params.param2}\"\n" +
                "        }\n" +
                "    }\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
        assertThat(systemOut.getLog(), not(containsString("[Pipeline] End of Pipeline")));
        assertThat(systemOut.getLog(), containsString("Finished: FAILURE"));
        assertThat(systemOut.getLog(), containsString("Unknown stage section"));
    }

    @Test
    public void shouldSupportDataboundMethods() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "node {\n" +
                "    checkout scm\n" +
                "}\n", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should be executed successfully", result, equalTo(0));
    }

    @Test
    public void helloWorldAsYaml() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile.yml");
        FileUtils.writeStringToFile(jenkinsfile,
                "pipeline:\n" +
                        "  agent:\n" +
                        "    none:\n" +
                        "  stages:\n" +
                        "    - stage: \"Print Hello\"\n" +
                        "      steps:\n" +
                        "        - echo \"Hello, world!\""
                , Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should be executed successfully", result, equalTo(0));
        assertThat(systemOut.getLog(), containsString("Hello, world!"));
    }

    @Test
    public void shouldFailWithABrokenConfig() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "echo 'Hello, world!'", Charset.defaultCharset());
        File jcasc = new File(tmp.getRoot(), "jenkins.yaml");
        Files.copy(SmokeTest.class.getResourceAsStream("SmokeTest/wrongConfig/jenkins.yaml"),
                jcasc.toPath());
        try {
            System.setProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY, jcasc.getAbsolutePath());
            int result = JFRTestUtil.runAsCLI(jenkinsfile);
            assertThat("Jenkinsfile Runner execution should have failed", result, not(equalTo(0)));
            assertThat(systemErr.getLog(), containsString("No configurator for the following root elements globalNodeProperties"));
        } finally {
            System.clearProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY);
        }
    }

    @Test
    public void checkoutSCM() throws Throwable {
        Map<String,String> filesAndContents = new HashMap<>();
        filesAndContents.put("README.md", "Test repository");

        File jenkinsfile = new File(getClass().getResource("SmokeTest/checkoutSCM/Jenkinsfile").getFile());
        String jfContent = FileUtils.readFileToString(jenkinsfile, Charset.defaultCharset());
        filesAndContents.put("Jenkinsfile", jfContent);

        String scmConfigPath = createTestRepoWithContentAndSCMConfigYAML(filesAndContents, "master");

        int result = JFRTestUtil.runAsCLI(jenkinsfile, Arrays.asList("--scm", scmConfigPath));
        assertThat("JFR should be executed successfully", result, equalTo(0));
        assertThat(systemOut.getLog(), containsString("README.md exists with content 'Test repository'"));
        assertThat(systemOut.getLog(), containsString("using credential user1"));
    }

    private String createTestRepoWithContentAndSCMConfigYAML(Map<String,String> filesAndContents, String branch) throws Exception {
        File gitDir = tmp.newFolder();
        FilePath gitDirPath = new FilePath(gitDir);
        GitClient git = Git.with(TaskListener.NULL, new EnvVars()).in(gitDir).getClient();
        git.init();
        PersonIdent johnDoe = new PersonIdent("John Doe", "john@doe.com");
        for (String fileName : filesAndContents.keySet()) {
            FilePath file = gitDirPath.child(fileName);
            try {
                file.write(filesAndContents.get(fileName), null);
            } catch (Exception e) {
                throw new GitException("unable to write file", e);
            }
            git.add(fileName);
        }
        git.setAuthor(johnDoe);
        git.setCommitter(johnDoe);
        git.commit("Test commit");

        Map<String,Object> remoteConfig = Stream.of(new Object[][]{
                { "url", gitDir.getAbsolutePath() },
                { "credentialsId", "user1" }
        }).collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

        Map<String,Object> config = new HashMap<>();
        config.put("scm",
                Collections.singletonMap("git",
                        Stream.of(new Object[][]{
                                { "userRemoteConfigs", Collections.singletonList(remoteConfig) },
                                { "branches", Collections.singletonList(Collections.singletonMap("name", branch)) }
                        }).collect(Collectors.toMap(data -> (String) data[0], data -> data[1]))));

        config.put("credential",
                Collections.singletonMap("usernamePassword",
                        Stream.of(new String[][]{
                                { "scope", "GLOBAL" },
                                { "id", "user1" },
                                { "username", "Administrator" },
                                { "password", "secret" },
                        }).collect(Collectors.toMap(data -> data[0], data -> data[1]))));

        File scmConfig = tmp.newFile();
        Yaml yaml = new Yaml();
        String asYaml = yaml.dump(config);
        FileUtils.writeStringToFile(scmConfig, asYaml, Charset.defaultCharset());

        return scmConfig.getAbsolutePath();
    }
}

