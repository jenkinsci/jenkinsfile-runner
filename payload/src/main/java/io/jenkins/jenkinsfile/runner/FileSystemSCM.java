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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    public void checkout(@Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {
        new FilePath(new File(dir)).copyRecursiveTo(new DirScanner.Glob("**/*", null, false), workspace, "**/*");
    }

    /**
     * Dummy implementation.
     *
     * Jenkinsfile Runner doesn't need to do polling, so this method is not needed.
     */
    @Override
    public SCMRevisionState calcRevisionsFromBuild(@Nonnull Run<?, ?> build, @Nullable FilePath workspace, @Nullable Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    /**
     * Dummy implementation.
     *
     * Jenkinsfile Runner doesn't need to do polling, so this method is not needed.
     */
    @Override
    public PollingResult compareRemoteRevisionWith(@Nonnull Job<?, ?> project, @Nullable Launcher launcher, @Nullable FilePath workspace, @Nonnull TaskListener listener, @Nonnull SCMRevisionState baseline) throws IOException, InterruptedException {
        return PollingResult.NO_CHANGES;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(FileSystemSCM.class);
    }

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<FileSystemSCM> {

        public DescriptorImpl() {
            super(FileSystemSCM.class, null);
            load();
        }

        public String getDisplayName() {
            return "FileSystemSCM";
        }

    }

}
