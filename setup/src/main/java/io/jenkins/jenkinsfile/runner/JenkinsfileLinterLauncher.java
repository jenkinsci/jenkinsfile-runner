package io.jenkins.jenkinsfile.runner;

import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.cli.shaded.org.apache.commons.io.FileUtils;
import io.jenkins.jenkinsfile.runner.bootstrap.commands.LintJenkinsfileCommand;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

/**
 * Set up of Jenkins environment for linting a single Jenkinsfile.
 */
public class JenkinsfileLinterLauncher extends JenkinsLauncher<LintJenkinsfileCommand> {

    public static final String CONVERTER_CLASS_NAME = "org.jenkinsci.plugins.pipeline.modeldefinition.parser.Converter";

    public JenkinsfileLinterLauncher(LintJenkinsfileCommand command) {
        super(command);
    }

    //TODO: add support of timeout

    /**
     * Launch the Jenkins instance
     * No time out and no output message
     */
    @Override
    protected int doLaunch() throws Exception {
        // So that the payload code has all the access to the system
        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            Class<?> cc = command.hasClass(CONVERTER_CLASS_NAME) ? Class.forName(CONVERTER_CLASS_NAME) : getConverterClassFromJar();
            try {
                // Attempt to call the scriptToPipelineDef method of the Converter class. This is the same as what
                // happens when a Jenkinsfile is POSTed to $JENKINS_URL/pipeline-model-converter/validate.
                System.out.println("Linting...");
                cc.getMethod("scriptToPipelineDef", String.class).invoke(cc.newInstance(), getJenkinsfileAsString());
                System.out.println("Done");
                return 0;
            } catch (Exception e) {
                // If the linting fails, we're expecting to catch an InvocationTargetException, which
                // wraps a MultipleCompilationErrorsException, which contains the linting errors.
                if (e instanceof InvocationTargetException) {
                    Throwable targetException = ((InvocationTargetException) e).getTargetException();
                    if (targetException instanceof MultipleCompilationErrorsException) {
                        ErrorCollector errorCollector = ((MultipleCompilationErrorsException) targetException).getErrorCollector();
                        if (errorCollector != null && errorCollector.hasErrors()) {
                            // Deliberately not using a try-with-resource here because, on close, I don't want to close down
                            // stdout.
                            PrintWriter pw = new PrintWriter(System.out, true);
                            for (Object error : errorCollector.getErrors()) {
                                if (error instanceof SyntaxErrorMessage) {
                                    ((SyntaxErrorMessage) error).write(pw, null);
                                }
                            }
                        }
                    }
                    // Return a non-zero exit code.
                    return 1;
                } else {
                    // Wasn't expecting this. Rethrow.
                    throw e;
                }
            }
        }
    }

    private String getJenkinsfileAsString() throws IOException {
        return FileUtils.readFileToString(command.pipelineLintOptions.jenkinsfile, StandardCharsets.UTF_8);
    }

    private Class<?> getConverterClassFromJar() throws IOException, ClassNotFoundException {
        return getClassFromJar(CONVERTER_CLASS_NAME);
    }
}
