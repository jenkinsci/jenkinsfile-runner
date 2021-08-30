package io.jenkins.jenkinsfile.runner.bootstrap;

import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;

public class BootstrapTest {

    @Test
    public void printsHelp() {
        assertCommandSuccess("--help");
    }

    @Test
    public void printsSubcommandHelp() {
        assertCommandSuccess("help", "cli");
        assertCommandSuccess("cli", "--help");
    }

    @Test
    public void printsVersion() {
        assertCommandSuccess("version");
        assertCommandSuccess("--version");
        assertCommandSuccess("-V");
    }

    @Test
    public void supportsMultipleArguments() {
        Bootstrap bootstrap = assertCommandSuccess("--arg=ARG1=value1", "--arg=ARG2=value2", "--version");
        assertEquals("Wrong number of parameters was parsed", bootstrap.pipelineRunOptions.workflowParameters.size(), 2);
    }

    private Bootstrap assertCommandSuccess(String ... args) throws AssertionError {
        final Bootstrap bootstrap = new Bootstrap();
        int exitCode = new CommandLine(bootstrap).execute(args);
        assertEquals("The command should have exited with the 0 exit code", exitCode, 0);
        return bootstrap;
    }
}