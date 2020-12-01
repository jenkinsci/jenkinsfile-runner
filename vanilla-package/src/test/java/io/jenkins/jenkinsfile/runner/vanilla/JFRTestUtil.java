package io.jenkins.jenkinsfile.runner.vanilla;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherCommand;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.JenkinsLauncherOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.PipelineRunOptions;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunJenkinsfileCommand;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

// TODO: convert to rule?

/**
 * Contains basic utility methods for testing Jenkinsfile Runner in unit tests
 */
public class JFRTestUtil {

    /**
     * Runs JFR using the low-level methods, {@link JenkinsLauncherCommand#postConstruct()} is skipped.
     */
    @CheckReturnValue
    public static int run(File jenkinsfile) throws Throwable {
        RunJenkinsfileCommand jfr = new RunJenkinsfileCommand();
        File vanillaTarget = new File("target");
        jfr.launcherOptions = new JenkinsLauncherOptions();
        jfr.pipelineRunOptions = new PipelineRunOptions();
        jfr.launcherOptions.warDir = new File(vanillaTarget, "war");
        jfr.launcherOptions.pluginsDir = new File(vanillaTarget, "plugins");
        jfr.pipelineRunOptions.jenkinsfile = jenkinsfile;

        //TODO: PostConstruct is not invoked
        return jfr.runJenkinsfileRunnerApp();
    }

    /**
     * Runs JFR using the CLI routines
     */
    @CheckReturnValue
    public static int runAsCLI(File jenkinsfile) throws Throwable {
        return runAsCLI(jenkinsfile, null);
    }


    /**
     * Runs JFR using the CLI routines
     */
    @CheckReturnValue
    public static int runAsCLI(File jenkinsfile, @CheckForNull Collection<String> additionalArgs) throws Throwable {
        File vanillaTarget = new File("target");
        File warDir = new File(vanillaTarget, "war");
        File pluginsDir = new File(vanillaTarget, "plugins");
        assertTrue("Jenkins WAR directory must exist when running tests", warDir.exists());
        assertTrue("Plugins directory must exist when running tests", pluginsDir.exists());

        List<String> basicArgs = Arrays.asList(
                "-w", warDir.getAbsolutePath(),
                "-p", pluginsDir.getAbsolutePath(),
                "-f", jenkinsfile.getAbsolutePath());
        List<String> cmd = new ArrayList<>(basicArgs);
        if (additionalArgs != null) {
            cmd.addAll(additionalArgs);
        }

        String[] args = cmd.toArray(new String[0]);
        return new CommandLine(new Bootstrap()).execute(args);
    }
}
