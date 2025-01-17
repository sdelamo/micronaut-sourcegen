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
package io.micronaut.sourcegen.generator.visitors.gradle;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.GenerateGradleTask;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTypeBuilder;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTypeBuilder.GradlePluginConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTypeBuilder.GradleTaskConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTypeBuilder.ParameterConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Gradle plugin generation.
 */
public final class GradlePluginUtils {

    static @NonNull GradleTypeBuilder.GradlePluginConfig getPluginConfig(
            @NonNull ClassElement element,
            @NonNull VisitorContext context
    ) {
        AnnotationValue<GenerateGradlePlugin> annotation = element.getAnnotation(GenerateGradlePlugin.class);

        List<GradleTaskConfig> taskConfigs = new ArrayList<>();
        for (AnnotationValue<GenerateGradleTask> taskAnn:
            annotation.getAnnotations("tasks", GenerateGradleTask.class)
        ) {
            taskConfigs.add(getTaskConfig(element, taskAnn, context));
        }

        return new GradlePluginConfig(
            taskConfigs,
            element.getPackageName(),
            annotation.stringValue("namePrefix").orElse(element.getSimpleName()),
            annotation.stringValue("taskGroup").orElse(null),
            annotation.booleanValue("micronautPlugin").orElse(true),
            annotation.stringValue("dependency").orElse(null),
            annotation.getRequiredValue("types", GenerateGradlePlugin.Type[].class)
        );
    }

    private static @NonNull GradleTaskConfig getTaskConfig(
            @NonNull ClassElement element,
            @NonNull AnnotationValue<GenerateGradleTask> annotation,
            @NonNull VisitorContext context
    ) {
        ClassElement source = annotation.stringValue("source")
            .flatMap(context::getClassElement).orElse(null);
        if (source == null) {
            throw new ProcessingException(element, "Could not load source type defined in @PluginGenerationTrigger");
        }

        List<MethodElement> executables = source.getMethods().stream()
            .filter(m -> m.hasAnnotation(PluginTaskExecutable.class))
            .toList();

        if (executables.size() != 1) {
            throw new ProcessingException(source, "Expected exactly one method annotated with @PluginTaskExecutable but found " + executables.size());
        }
        if (executables.get(0).getParameters().length != 0) {
            throw new ProcessingException(source, "Expected @PluginTaskExecutable method to have no parameters");
        }
        if (!executables.get(0).getReturnType().isVoid()) {
            throw new ProcessingException(source, "Expected @PluginTaskExecutable to have void return type");
        }
        String methodName = executables.get(0).getName();

        List<ParameterConfig> parameters = new ArrayList<>();
        for (PropertyElement property: source.getBeanProperties()) {
            parameters.add(getParameterConfig(property));
        }

        return new GradleTaskConfig(
            source,
            parameters,
            methodName,
            annotation.stringValue("namePrefix").orElse(source.getSimpleName()),
            annotation.stringValue("extensionMethodName").orElse(methodName)
        );
    }

    private static @NonNull ParameterConfig getParameterConfig(@NonNull PropertyElement property) {
        AnnotationValue<PluginTaskParameter> annotation = property.getAnnotation(PluginTaskParameter.class);
        if (annotation == null) {
            return new ParameterConfig(property, false, null, false, false, false);
        }
        return new ParameterConfig(
            property,
            annotation.booleanValue("required").orElse(false),
            annotation.stringValue("defaultValue").orElse(null),
            annotation.booleanValue("internal").orElse(false),
            annotation.booleanValue("directory").orElse(false),
            annotation.booleanValue("output").orElse(false)
        );
    }

}
