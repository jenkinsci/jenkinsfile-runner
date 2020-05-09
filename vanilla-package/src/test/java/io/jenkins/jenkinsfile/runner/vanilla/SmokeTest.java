package io.jenkins.jenkinsfile.runner.vanilla;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class SmokeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void helloWorld() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "echo 'Hello, world!'", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile);
        assertThat("JFR should be executed successfully", result, equalTo(0));
    }

    @Test
    public void shouldFailWithWrongJenkinsfile() throws Throwable {
        File jenkinsfile = new File(tmp.getRoot(), "Jenkinsfile");

        int result = JFRTestUtil.run(jenkinsfile);
        assertThat("JFR should fail when there is no Jenkinsfile", result, not(equalTo(0)));
    }
}
