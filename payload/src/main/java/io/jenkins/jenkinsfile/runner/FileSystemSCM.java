package io.jenkins.jenkinsfile.runner;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.DirScanner;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class FileSystemSCM extends SCM {
    private final String dir;

    @DataBoundConstructor
    public FileSystemSCM(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return this.dir;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return NullChangeLogParser.INSTANCE;
    }

    @Override
    public void checkout(@NonNull Run<?, ?> build, @NonNull Launcher launcher, @NonNull FilePath workspace, @NonNull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {
        new FilePath(new File(dir == null ? "." : dir))
                .copyRecursiveTo(new DirScanner.Glob("**/*", null, false), workspace, "**/*");
    }

    /**
     * Dummy implementation.
     *
     * Jenkinsfile Runner doesn't need to do polling, so this method is not needed.
     */
    @Override
    public SCMRevisionState calcRevisionsFromBuild(@NonNull Run<?, ?> build, @Nullable FilePath workspace, @Nullable Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    /**
     * Dummy implementation.
     *
     * Jenkinsfile Runner doesn't need to do polling, so this method is not needed.
     */
    @Override
    public PollingResult compareRemoteRevisionWith(@NonNull Job<?, ?> project, @Nullable Launcher launcher, @Nullable FilePath workspace, @NonNull TaskListener listener, @NonNull SCMRevisionState baseline) throws IOException, InterruptedException {
        return PollingResult.NO_CHANGES;
    }

    // TO-DO: Remove once JENKINS-55323 is fixed
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR_INSTANCE;
    }

    private final static DescriptorImpl DESCRIPTOR_INSTANCE = new DescriptorImpl();

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<FileSystemSCM> {

        public DescriptorImpl() {
            super(FileSystemSCM.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "FileSystemSCM";
        }

    }

}
