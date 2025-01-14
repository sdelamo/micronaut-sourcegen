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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface for a Gradle plugin builder type.
 */
public interface PluginBuilder {

    @NonNull GenerateGradlePlugin.Type getType();

    @NonNull List<ObjectDef> build(@NonNull PluginBuilder.GradleTaskConfig taskConfig);

    static @NonNull PluginBuilder.GradleTaskConfig getTaskConfig(@NonNull ClassElement type, @NonNull AnnotationValue<GenerateGradlePlugin> annotation) {
        List<MethodElement> executables = type.getMethods().stream()
            .filter(m -> m.hasAnnotation(PluginTaskExecutable.class))
            .toList();
        if (executables.size() != 1) {
            throw new ProcessingException(type, "Expected exactly one method annotated with @PluginTaskExecutable but found " + executables.size());
        }
        if (executables.get(0).getParameters().length != 0) {
            throw new ProcessingException(type, "Expected @PluginTaskExecutable method to have no parameters");
        }
        if (!executables.get(0).getReturnType().isVoid()) {
            throw new ProcessingException(type, "Expected @PluginTaskExecutable to have void return type");
        }
        String executable = executables.get(0).getName();

        List<ParameterConfig> parameters = new ArrayList<>();
        for (PropertyElement property: type.getBeanProperties()) {
            parameters.add(getParameterConfig(property));
        }

        return new GradleTaskConfig(
            type,
            parameters,
            executable,
            type.getPackageName(),
            annotation.stringValue("namePrefix").orElse(type.getSimpleName()),
            annotation.stringValue("extensionMethodName").orElse(null),
            annotation.stringValue("taskGroup").orElse(null),
            annotation.booleanValue("micronautPlugin").orElse(true),
            annotation.stringValue("dependency").orElse(null),
            annotation.getRequiredValue("types", GenerateGradlePlugin.Type[].class)
        );
    }

    private static @NonNull ParameterConfig getParameterConfig(@NonNull PropertyElement property) {
        AnnotationValue<PluginTaskParameter> annotation = property.getAnnotation(PluginTaskParameter.class);
        if (annotation == null) {
            return new ParameterConfig(property, false, null, false, false);
        }
        return new ParameterConfig(
            property,
            annotation.booleanValue("required").orElse(false),
            annotation.stringValue("defaultValue").orElse(null),
            annotation.booleanValue("internal").orElse(false),
            annotation.booleanValue("directory").orElse(false)
        );
    }

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
     * @param isDirectory Whether it is a directory
     */
    record ParameterConfig(
        PropertyElement source,
        boolean required,
        String defaultValue,
        boolean internal,
        boolean isDirectory
    ) {
    }

}
