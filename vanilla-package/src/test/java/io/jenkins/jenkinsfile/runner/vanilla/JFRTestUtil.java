package io.jenkins.jenkinsfile.runner.vanilla;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
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
     * Runs JFR using the low-level methods, {@link Bootstrap#postConstruct(CmdLineParser)} is skipped.
     */
    @CheckReturnValue
    public static int run(File jenkinsfile) throws Throwable {
        Bootstrap jfr = new Bootstrap();
        File vanillaTarget = new File(FilenameUtils.getName("target"));
        jfr.warDir = new File(vanillaTarget, FilenameUtils.getName("war"));
        jfr.pluginsDir = new File(vanillaTarget, FilenameUtils.getName("plugins"));
        jfr.jenkinsfile = jenkinsfile;

        //TODO: PostConstruct is not invoked
        return jfr.run();
    }

    /**
     * Runs JFR using the CLI routines
     */
    @CheckReturnValue
    public static int runAsCLI(File jenkinsfile) throws Throwable, CmdLineException {
        return runAsCLI(jenkinsfile, null);
    }


    /**
     * Runs JFR using the CLI routines
     */
    @CheckReturnValue
    public static int runAsCLI(File jenkinsfile, @CheckForNull Collection<String> additionalArgs) throws Throwable, CmdLineException {
        File vanillaTarget = new File(FilenameUtils.getName("target"));
        File warDir = new File(vanillaTarget, FilenameUtils.getName("war"));
        File pluginsDir = new File(vanillaTarget, FilenameUtils.getName("plugins"));
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

        final Bootstrap bootstrap = new Bootstrap();
        CmdLineParser parser = new CmdLineParser(bootstrap);
        parser.parseArgument(args);
        bootstrap.postConstruct(parser);
        return bootstrap.run();
    }
}
