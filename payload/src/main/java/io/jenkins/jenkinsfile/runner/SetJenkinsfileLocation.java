package io.jenkins.jenkinsfile.runner;

import hudson.model.Action;
import hudson.model.InvisibleAction;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.CpsFlowFactoryAction2;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Loads {@code Jenkinsfile} from a specific file.
 *
 * @author Kohsuke Kawaguchi
 */
public class SetJenkinsfileLocation extends InvisibleAction implements CpsFlowFactoryAction2 {
    private final File jenkinsfile;
    private final Boolean sandboxedExecution;

    public SetJenkinsfileLocation(File jenkinsfile, Boolean sandbox) {
        this.jenkinsfile = jenkinsfile;
        this.sandboxedExecution = sandbox;
    }

    @Override
    public CpsFlowExecution create(FlowDefinition def, FlowExecutionOwner owner, List<? extends Action> actions) throws IOException {
        return new CpsFlowExecution(FileUtils.readFileToString(jenkinsfile), this.sandboxedExecution, owner);
    }
}
