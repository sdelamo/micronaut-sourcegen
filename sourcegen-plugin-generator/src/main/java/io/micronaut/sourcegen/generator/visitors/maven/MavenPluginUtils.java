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
package io.micronaut.sourcegen.generator.visitors.maven;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.GenerateMavenMojo;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils class for Maven plugin generation.
 */
final class MavenPluginUtils {

    static @NonNull List<MavenTaskConfig> getTaskConfigs(
        @NonNull ClassElement element, @NonNull VisitorContext context
    ) {
        List<AnnotationValue<GenerateMavenMojo>> annotations =
            element.getAnnotationValuesByType(GenerateMavenMojo.class);
        return annotations.stream().map(a -> getTaskConfig(element, a, context)).toList();
    }

    private static @NonNull MavenTaskConfig getTaskConfig(
            @NonNull ClassElement element, @NonNull AnnotationValue<GenerateMavenMojo> annotation, @NonNull VisitorContext context
    ) {
        ClassElement source = annotation.stringValue("source")
            .flatMap(context::getClassElement).orElse(null);
        if (source == null) {
            throw new ProcessingException(element, "Could not load source type defined in @GenerateMavenMojo");
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

        return new MavenTaskConfig(
            source,
            parameters,
            methodName,
            element.getPackageName(),
            annotation.stringValue("namePrefix").orElse(element.getSimpleName()),
            annotation.booleanValue("micronautPlugin").orElse(true),
            annotation.stringValue("mavenPropertyPrefix").orElse(null)
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

    /**
     * Configuration for a gradle task type.
     *
     * @param source The configuration source
     * @param parameters The parameters
     * @param methodName The run method name
     * @param packageName The package name
     * @param namePrefix The type name prefix
     * @param micronautPlugin Whether to extend micronaut plugin
     * @param mavenPropertyPrefix The prefix for maven properties
     */
    public record MavenTaskConfig(
        ClassElement source,
        List<ParameterConfig> parameters,
        String methodName,
        String packageName,
        String namePrefix,
        boolean micronautPlugin,
        String mavenPropertyPrefix
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
    public record ParameterConfig(
        PropertyElement source,
        boolean required,
        String defaultValue,
        boolean internal,
        boolean directory,
        boolean output
    ) {
    }

}
