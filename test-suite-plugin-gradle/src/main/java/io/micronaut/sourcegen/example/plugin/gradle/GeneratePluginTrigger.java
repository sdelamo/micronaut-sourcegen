/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.example.plugin.gradle;

import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.GenerateGradleTask;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.Type;

@GenerateGradlePlugin(
    namePrefix = "Test",
    micronautPlugin = false,
    types = {
        Type.GRADLE_TASK,
        Type.GRADLE_EXTENSION,
        Type.GRADLE_SPECIFICATION,
        Type.GRADLE_PLUGIN
    },
    tasks = {
        @GenerateGradleTask(
            namePrefix = "GenerateSimpleRecord",
            extensionMethodName = "generateSimpleRecord",
            source = "io.micronaut.sourcegen.example.plugin.GenerateSimpleRecordTask"
        ),
        @GenerateGradleTask(
            namePrefix = "GenerateSimpleResource",
            extensionMethodName = "generateSimpleResource",
            source = "io.micronaut.sourcegen.example.plugin.GenerateSimpleResourceTask"
        )
    }
)
public final class GeneratePluginTrigger {
}
