package io.jenkins.jenkinsfile.runner.bootstrap.commands;

import io.jenkins.jenkinsfile.runner.bootstrap.Util;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

// TODO: Add support for printing Jenkins core and Pipeline versions? Will need to booth the Jenkins instance
@Command(name = "version", description = "Shows Jenkinsfile Runner version", mixinStandardHelpOptions = true)
public class VersionCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println(Util.getJenkinsfileRunnerVersion());
        return 0;
    }
}
