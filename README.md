# Jenkinsfile Runner
Jenkinsfile Runner is an experiment to package Jenkins pipeline execution as a command line tool.
This allows Jenkins to be used as Function-as-a-Service.

## Build
Currently there's no released distribution, so you must first build this code:
```
mvn package
```
This will produce the distribution in `app/target/appassembler`

## Usage
