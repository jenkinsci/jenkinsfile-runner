package io.jenkins.jenkinsfile.runner.vanilla;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
        File vanillaTarget = new File("target");
        jfr.warDir = new File(vanillaTarget, "war");
        jfr.pluginsDir = new File(vanillaTarget, "plugins");
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
        File vanillaTarget = new File("target");
        List<String> basicArgs = Arrays.asList(
                "-w", new File(vanillaTarget, "war").getAbsolutePath(),
                "-p", new File(vanillaTarget, "plugins").getAbsolutePath(),
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
