package io.jenkins.jenkinsfile.runner;

import hudson.cli.CLICommand;
import hudson.security.ACL;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.RunCLICommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sets up a Jenkins environment that provides an interactive CLI.
 */
public class CLILauncher extends JenkinsLauncher<RunCLICommand> {
    public CLILauncher(RunCLICommand command) {
        super(command);
    }

    @Override
    protected int doLaunch() throws Exception {
        // so that the CLI has all the access to the system
        ACL.impersonate(ACL.SYSTEM);
        BufferedReader commandIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        System.out.printf("Connected to Jenkins!%nType 'help' for a list of available commands, or 'exit' to quit.%n");
        System.out.print(" > ");
        while ((line = commandIn.readLine()) != null) {
            if(line.equalsIgnoreCase("exit")) {
                break;
            } else if(line.isEmpty()) {
                continue;
            }
            tryRunCommand(line);
            System.out.print(" > ");
        }
        System.out.println("bye");
        return 0;
    }

    private void tryRunCommand(String commandLine) {
        try {
            String[] parts = commandLine.split(" ", 2);
            CLICommand command = CLICommand.clone(parts[0]);
            if(command == null) {
                System.err.println("No such command, try 'help'.");
                return;
            }
            command.main(findArgsFromLineParts(parts), Locale.getDefault(), System.in, System.out, System.err);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An unexpected error occurred executing this command.");
        }
    }

    private List<String> findArgsFromLineParts(String[] commandLineParts) {
        if(commandLineParts.length > 1) {
            return Arrays.asList(commandLineParts[1].split(" "));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected String getThreadName() {
        return "Jenkins CLI";
    }
}
