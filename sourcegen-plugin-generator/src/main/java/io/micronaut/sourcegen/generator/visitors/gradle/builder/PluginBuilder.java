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
package io.micronaut.sourcegen.generator.visitors.gradle.builder;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.List;

/**
 * An interface for a Gradle plugin builder type.
 */
public interface PluginBuilder {

    @NonNull GenerateGradlePlugin.Type getType();

    @NonNull List<ObjectDef> build(@NonNull PluginBuilder.GradleTaskConfig taskConfig);

    /**
     * Configuration for a gradle task type.
     *
     * @param source The configuration source
     * @param parameters The parameters
     * @param methodName The run method name
     * @param packageName The package name
     * @param namePrefix The type name prefix
     * @param gradleExtensionMethodName The extension method name
     * @param gradleGroup The gradle group to use
     * @param micronautPlugin Whether to extend micronaut plugin
     * @param dependency The dependency
     * @param types The types to generate
     */
    record GradleTaskConfig(
        ClassElement source,
        List<ParameterConfig> parameters,
        String methodName,
        String packageName,
        String namePrefix,
        String gradleExtensionMethodName,
        String gradleGroup,
        boolean micronautPlugin,
        String dependency,
        GenerateGradlePlugin.Type[] types
    ) {
    }

    /**
     * Configuration for a plugin parameter.
     *
     * @param source The source parameter
     * @param required Whether it is required
     * @param defaultValue The default value
     * @param internal Whether it is internal
     * @param directory Whether it is a directory
     * @param output Whether it is an output
     */
    record ParameterConfig(
        PropertyElement source,
        boolean required,
        String defaultValue,
        boolean internal,
        boolean directory,
        boolean output
    ) {
    }

}
