Job Folders demo
=================

This demo shows, how to define the Job inside of a folder-structure, defined by parameter ```--job-name```  
To create folders with additional behavior (e.g. loading of shared libraries), Job DSLs as JCasC can be used.

You can run this demos after building jenkinsfile-runner image as described in [Contributing to Jenkinsfile Runner](/CONTRIBUTING.adoc#building)


## Run with created Folder by JCasC (with Docker)  

To run in folders without assigned shared libraries  

```bash
docker run --rm \
	-v $(pwd)/demo/job-folders/Jenkinsfile:/workspace/Jenkinsfile \
	-v $(pwd)/demo/job-folders/folderTemplate.yaml:/usr/share/jenkins/ref/casc/folderTemplate.yaml \
	-v $(pwd)/vanilla-package/target/plugins:/usr/share/jenkins/ref/plugins \
	jenkins4eval/jenkinsfile-runner:dev \
	--job-name folderWithoutLib/exampleJob
```	

To run in folders with assigned shared libraries  

```bash
docker run --rm \
	-v $(pwd)/demo/job-folders/JenkinsfileWithLibraryCall:/workspace/Jenkinsfile \
	-v $(pwd)/demo/job-folders/folderTemplate.yaml:/usr/share/jenkins/ref/casc/folderTemplate.yaml \
	-v $(pwd)/vanilla-package/target/plugins:/usr/share/jenkins/ref/plugins \
	jenkins4eval/jenkinsfile-runner:dev \
	--job-name myFolder/myJob
```
	
## Create folder without JCasC (with Docker)

```bash
docker run --rm \
	-v $(pwd)/demo/job-folders/Jenkinsfile:/workspace/Jenkinsfile \
	jenkins4eval/jenkinsfile-runner:dev \
	--job-name folder1/folder2/myJob
```	

## Run named job without folders (with Docker)

```bash
docker run --rm \
	-v $(pwd)/demo/job-folders/Jenkinsfile:/workspace/Jenkinsfile \
	jenkins4eval/jenkinsfile-runner:dev \
	--job-name myJob
```

## Run without defined name (with Docker)
```bash
docker run --rm \
	-v $(pwd)/demo/job-folders/Jenkinsfile:/workspace/Jenkinsfile \
	jenkins4eval/jenkinsfile-runner:dev
```
