package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import picocli.CommandLine;

import java.io.File;

/**
 * Arguments needed to launch a Jenkins instance.
 * @see JenkinsLauncherCommand
 */
public class JenkinsLauncherOptions {

    /**
     * Exploded jenkins.war
     */
    @CommandLine.Option(names = { "-w", "--jenkins-war" },
            description = "Path to exploded jenkins war directory." +
                    "Depending on packaging, it may contain the entire WAR " +
                    "or just resources to be loaded by the WAR file, for example Groovy hooks or extra libraries.")
    @CheckForNull
    public File warDir;

    /**
     * Where to load plugins from?
     */
    @CommandLine.Option(names = { "-p", "--plugins" },
            description = "Plugins required to run pipeline. Either a plugins.txt file or a /plugins installation directory. Defaults to plugins.txt")
    public File pluginsDir;

    @CommandLine.Option(names = { "-jv", "--jenkins-version"},
            description = "Jenkins version to use if Jenkins WAR is not specified by --jenkins-war. Defaults to the latest LTS")
    @CheckForNull
    public String version;

    @CommandLine.Option(names = { "-m", "--mirror"},
            description = "Download mirror site of Jenkins, defaults to http://updates.jenkins.io/download. Get the mirror list from http://mirrors.jenkins-ci.org/status.html")
    public String mirror;

    /**
     * Jenkins Home for the Run.
     */
    @CheckForNull
    @CommandLine.Option(names = {"--jenkinsHome", "--runHome"},
            description = "Path to the empty Jenkins Home directory to use for this run. If not specified a temporary directory will be created. " +
                    "Note that the specified folder will not be disposed after the run")
    public File jenkinsHome;

    @CheckForNull
    @CommandLine.Option(names = "--withInitHooks",
            description = "Path to a directory containing Groovy Init Hooks to copy into init.groovy.d")
    public File withInitHooks;

    @CommandLine.Option(names = "--skipShutdown",
            description = "Forces Jenkinsfile Runner to skip the shutdown logic. " +
                    "It reduces the instance termination time but may lead to unexpected behavior in plugins " +
                    "which release external resources on clean up synchronous task queues on shutdown.")
    public boolean skipShutdown;

    @CheckForNull
    @CommandLine.Option(names = "--libPath",
            description = "When a slim packaging is used, points to the library directory which contains payload.jar and setup.jar files")
    public File libPath;


    public String getMirrorURL(String url) {
        if (this.mirror == null || "".equals(this.mirror.trim())) {
            return url;
        }

        return url.replace("https://updates.jenkins.io/download", this.mirror);
    }
}
