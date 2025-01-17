/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.sourcegen.example.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * This extends the generated plugin to make sure that the correct extension class is used.
 * Only this plugin is registered with gradle, not the generated one.
 */
public class TestPluginImpl extends TestPlugin {

    @Override
    protected TestExtension createExtension(Project project, Configuration classpath) {
        return project.getExtensions().create(
            TestExtensionImpl.class, "test", TestExtensionImpl.class, project, classpath
        );
    }
}
