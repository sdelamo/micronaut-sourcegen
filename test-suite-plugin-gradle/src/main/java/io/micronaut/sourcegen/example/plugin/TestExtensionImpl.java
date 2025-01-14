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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.function.Consumer;

public abstract class TestExtensionImpl extends DefaultTestExtension {

    public TestExtensionImpl(Project project, Configuration classpath) {
        super(project, classpath);
    }

    @Override
    TaskProvider<? extends TestTask> createTask(String name, TestTaskConfigurator configurator) {
        TaskProvider<TestTaskImpl> task = project.getTasks().register(name, TestTaskImpl.class, t -> {
            configurator.execute(t);
            t.getOutputFolder().convention(
                project.getLayout().getBuildDirectory().dir("generated/" + t.getName())
            );
        });
        withJavaSourceSets(sourceSets -> {
            var javaMain = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava();
            javaMain.srcDir(task.map(t -> t.getOutputFolder().dir("src/main/java")));
        });
        return task;
    }

    private void withJavaSourceSets(Consumer<? super SourceSetContainer> consumer) {
        project.getPlugins().withId("java", unused -> {
            var javaPluginExtension =  project.getExtensions().findByType(JavaPluginExtension.class);
            if (javaPluginExtension == null) {
                throw new GradleException("No Java plugin extension found");
            }
            consumer.accept(javaPluginExtension.getSourceSets());
        });
    }

}
