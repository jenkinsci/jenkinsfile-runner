package io.jenkins.jenkinsfile.runner.vanilla;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.io.File;
import java.nio.charset.Charset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class SmokeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog();

    @Rule
    public final SystemErrRule systemErr = new SystemErrRule().enableLog();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(30);

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


}
