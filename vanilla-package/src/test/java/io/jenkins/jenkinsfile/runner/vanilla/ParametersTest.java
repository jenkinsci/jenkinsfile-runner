package io.jenkins.jenkinsfile.runner.vanilla;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies support of parameters in Jenkinsfile Runner
 */
public class ParametersTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(120);

    @Test
    public void scriptedPipeline() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "properties([\n" +
                "  [$class: 'ParametersDefinitionProperty', parameterDefinitions: [\n" +
                "    [$class: 'StringParameterDefinition',\n" +
                "      name: 'param1',\n" +
                "      defaultValue: '',\n" +
                "      description: 'Greeting message'],\n" +
                "    [$class: 'StringParameterDefinition',\n" +
                "      name: 'param2',\n" +
                "      defaultValue: '',\n" +
                "      description: '2nd parameter']\n" +
                "  ]]\n" +
                "])\n" +
                "\n" +
                "node {\n" +
                "    echo 'Hello, world!'\n" +
                "    echo \"Value for param1: ${params.param1}\"\n" +
                "    echo \"Value for param2: ${params.param2}\"\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile, Arrays.asList("-a", "param1=Hello", "-a", "param2=value2"));
        assertThat("JFR should be executed successfully", result, equalTo(0));
        assertThat(systemOut.getLog(), containsString("Hello, world!"));
        assertThat(systemOut.getLog(), containsString("Value for param1: Hello"));
        assertThat(systemOut.getLog(), containsString("Value for param2: value2"));
    }

    @Test
    public void declarativePipeline() throws Throwable {
        File jenkinsfile = tmp.newFile("Jenkinsfile");
        FileUtils.writeStringToFile(jenkinsfile, "pipeline {\n" +
                "    agent any\n" +
                "    parameters {\n" +
                "        string(name: 'param1', defaultValue: '', description: 'Greeting message')\n" +
                "        string(name: 'param2', defaultValue: '', description: '2nd parameter')\n" +
                "    }\n" +
                "    stages {\n" +
                "        stage('Build') {\n" +
                "            steps {\n" +
                "                echo 'Hello, world!'\n" +
                "                echo \"Value for param1: ${params.param1}\"\n" +
                "                echo \"Value for param2: ${params.param2}\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}", Charset.defaultCharset());

        int result = JFRTestUtil.runAsCLI(jenkinsfile, Arrays.asList("-a", "param1=Hello", "-a", "param2=value2"));
        assertThat("JFR should be executed successfully", result, equalTo(0));
        assertThat(systemOut.getLog(), containsString("Hello, world!"));
        assertThat(systemOut.getLog(), containsString("Value for param1: Hello"));
        assertThat(systemOut.getLog(), containsString("Value for param2: value2"));
    }
}
