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
package io.micronaut.sourcegen.generator.visitors;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;

import java.util.List;

/**
 * Common utility methods for plugin generation.
 */
@Internal
public class PluginUtils {

    public static @NonNull ParameterConfig getParameterConfig(@NonNull PropertyElement property) {
        AnnotationValue<PluginTaskParameter> annotation = property.getAnnotation(PluginTaskParameter.class);
        if (annotation == null) {
            return new ParameterConfig(property, false, null, false, false, false, null);
        }
        return new ParameterConfig(
            property,
            annotation.booleanValue("required").orElse(false),
            annotation.stringValue("defaultValue").orElse(null),
            annotation.booleanValue("internal").orElse(false),
            annotation.booleanValue("directory").orElse(false),
            annotation.booleanValue("output").orElse(false),
            annotation.stringValue("globalProperty").orElse(null)
        );
    }

    /**
     * Validate and get the method name of the task executable.
     *
     * @param source The source element annotated with {@link io.micronaut.sourcegen.annotations.PluginTask}.
     * @return The method name
     */
    public static String getTaskExecutableMethodName(ClassElement source) {
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
        return executables.get(0).getName();
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
     * @param globalProperty A global property
     */
    public record ParameterConfig(
        @NonNull PropertyElement source,
        boolean required,
        @Nullable String defaultValue,
        boolean internal,
        boolean directory,
        boolean output,
        @Nullable String globalProperty
    ) {
    }

}
