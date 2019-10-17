package io.jenkins.jenkinsfile.runner;

/**
 * @author Matthias Rinck
 */
class StringCause extends hudson.model.Cause {

  private String cause;
  
  public StringCause(String cause) {
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
    if (!(o instanceof StringCause)) return false;
    final StringCause c = (StringCause) o;
    return this.cause.equals(c.getShortDescription());
  }

  @Override
  public int hashCode() {
    return this.cause.hashCode();
  }
}
