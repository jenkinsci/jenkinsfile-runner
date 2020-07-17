
if(![System.String]::IsNullOrWhiteSpace($env:DEBUG)) {
  $env:JAVA_OPTS=('-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 {0}' -f $env:JAVA_OPTS)
}

& C:/app/bin/jenkinsfile-runner.bat $args
exit $LastExitCode
