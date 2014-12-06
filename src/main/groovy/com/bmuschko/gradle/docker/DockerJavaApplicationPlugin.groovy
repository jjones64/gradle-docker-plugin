/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Tar

/**
 * Opinonated Gradle plugin for creating and pushing a Docker image for a Java application.
 */
class DockerJavaApplicationPlugin implements Plugin<Project> {
    static final String COPY_DIST_TAR_TASK_NAME = 'dockerCopyDistTar'
    static final String DOCKERFILE_TASK_NAME = 'dockerDistTar'
    static final String BUILD_IMAGE_TASK_NAME = 'dockerBuildImage'
    static final String PUSH_IMAGE_TASK_NAME = 'dockerPushImage'

    @Override
    void apply(Project project) {
        project.plugins.apply(DockerRemoteApiPlugin)

        project.plugins.withType(ApplicationPlugin) {
            Tar tarTask = project.tasks.getByName(ApplicationPlugin.TASK_DIST_TAR_NAME)

            Dockerfile createDockerfileTask = createDockerfileTask(project, tarTask)
            Copy copyTarTask = createCopyTarTask(project, tarTask, createDockerfileTask)
            createDockerfileTask.dependsOn copyTarTask
            DockerBuildImage dockerBuildImageTask = createBuildImageTask(project, createDockerfileTask)
            createPushImageTask(project, dockerBuildImageTask)
        }
    }

    private Dockerfile createDockerfileTask(Project project, Tar tarTask) {
        project.task(DOCKERFILE_TASK_NAME, type: Dockerfile) {
            dependsOn tarTask
            from 'dockerfile/java'
            addFile tarTask.archivePath.name, '/'
            entryPoint determineEntryPoint(tarTask)
            exposePort 8080
        }
    }

    private Copy createCopyTarTask(Project project, Tar tarTask, Dockerfile createDockerfileTask) {
        project.task(COPY_DIST_TAR_TASK_NAME, type: Copy) {
            group = DockerRemoteApiPlugin.DEFAULT_TASK_GROUP
            dependsOn tarTask
            from { tarTask.archivePath }
            into { createDockerfileTask.destFile.parentFile }
        }
    }

    private String determineEntryPoint(Tar tarTask) {
        String archiveNameWithoutFileExtension = tarTask.archiveName - ".${tarTask.extension}"
        "/$archiveNameWithoutFileExtension"
    }

    private DockerBuildImage createBuildImageTask(Project project, Dockerfile createDockerfileTask) {
        project.task(BUILD_IMAGE_TASK_NAME, type: DockerBuildImage) {
            dependsOn createDockerfileTask
            conventionMapping.inputDir = { createDockerfileTask.destFile.parentFile }
            conventionMapping.tag = {
                String tagVersion = project.version == 'unspecified' ? 'latest' : project.version
                "${project.applicationName}:${tagVersion}".toString()
            }
        }
    }

    private void createPushImageTask(Project project, DockerBuildImage dockerBuildImageTask) {
        project.task(PUSH_IMAGE_TASK_NAME, type: DockerPushImage) {
            dependsOn dockerBuildImageTask
            targetImageId { dockerBuildImageTask.imageId }
        }
    }
}