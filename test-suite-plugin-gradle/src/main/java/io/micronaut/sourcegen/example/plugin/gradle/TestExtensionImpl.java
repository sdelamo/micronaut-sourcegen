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
package io.micronaut.sourcegen.example.plugin.gradle;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.function.Consumer;

/**
 * This extends a generated class to modify some behavior.
 */
public abstract class TestExtensionImpl extends DefaultTestExtension {

    public TestExtensionImpl(Project project, Configuration classpath) {
        super(project, classpath);
    }

    /**
     * This is an example of how you can add a utility method to the generated extension.
     * Inside it calls the generated method.
     *
     * @param typeName The type name
     * @param packageName The package name
     * @param action The spec action
     */
    public void generateRecordWithName(String typeName, String packageName, Action<GenerateSimpleRecordSpec> action) {
        super.generateSimpleRecord("generate" + typeName, spec -> {
            spec.getTypeName().set(typeName);
            spec.getPackageName().set(packageName);
            action.execute(spec);
        });
    }

    /**
     * This is another example of a utility method.
     *
     * @param name The task name
     * @param fileName The file name
     * @param content The content
     */
    public void generateResource(String name, String fileName, String content) {
        super.generateSimpleResource(name, spec -> {
            spec.getFileName().set(fileName);
            spec.getContent().set(content);
        });
    }

    /**
     * Overriding a method to make sure that output directory has a correct default value.
     * We are also adding to source sets here.
     *
     * @param name The task name
     * @param configurator The configurator action
     * @return The task
     */
    @Override
    TaskProvider<? extends GenerateSimpleRecordTask> createGenerateSimpleRecordTask(
            String name, Action<GenerateSimpleRecordTask> configurator
    ) {
        TaskProvider<? extends GenerateSimpleRecordTask> task = super.createGenerateSimpleRecordTask(name, t -> {
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

    /**
     * Overriding a method to make sure that output directory has a correct default value.
     * We are also adding to resource sets here.
     *
     * @param name The task name
     * @param configurator The configurator action
     * @return The task
     */
    @Override
    TaskProvider<? extends GenerateSimpleResourceTask> createGenerateSimpleResourceTask(
        String name, Action<GenerateSimpleResourceTask> configurator
    ) {
        TaskProvider<? extends GenerateSimpleResourceTask> task = super.createGenerateSimpleResourceTask(name, t -> {
            configurator.execute(t);
            t.getOutputFolder().convention(
                project.getLayout().getBuildDirectory().dir("generated/" + t.getName())
            );
        });
        withJavaSourceSets(sourceSets -> {
            var resources = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources();
            resources.srcDir(task.map(GenerateSimpleResourceTask::getOutputFolder));
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
