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
package com.bmuschko.gradle.docker.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class AbstractReactiveStreamsTask extends ConventionTask {

    /**
     * Closure to handle the possibly throw exception
     */
    @Internal
    Closure onError

    /**
     * Closure to handle results
     */
    @Internal
    Closure onNext

    /**
     * Closure to handle task completion
     */
    @Internal
    Closure onComplete

    @TaskAction
    void start() {
        boolean commandFailed = false
        try {
            runReactiveStream()
        } catch (Exception possibleException) {
            commandFailed = true
            if (onError) {
                onError(possibleException)
            } else {
                throw possibleException
            }
        }
        if(!commandFailed && onComplete) {
            onComplete()
        }
    }

    abstract void runReactiveStream()
}
