package io.jenkins.jenkinsfile.runner;

/**
 * @author Matthias Rinck
 */
class JenkinsfileRunnerCause extends hudson.model.Cause {

  private String cause;
  
  public JenkinsfileRunnerCause(String cause) {
    this.cause = cause;
  }

  @Override
  public String getShortDescription()
  {
    return String.format("Started by %s",cause);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JenkinsfileRunnerCause)) return false;
    final JenkinsfileRunnerCause c = (JenkinsfileRunnerCause) o;
    return this.cause.equals(c.getShortDescription());
  }

  @Override
  public int hashCode() {
    return this.cause.hashCode();
  }
}
