package io.jenkins.jenkinsfile.runner;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class FileSystemSCM extends SCM {
    private final File dir;

    public FileSystemSCM(File dir) {
        this.dir = dir;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return NullChangeLogParser.INSTANCE;
    }

    @Override
    public void checkout(@Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {
        new FilePath(dir).copyRecursiveTo(workspace);
    }
}
