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
package io.micronaut.sourcegen.generator.visitors.builder;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.sourcegen.annotations.PluginGenerationTrigger;
import io.micronaut.sourcegen.annotations.PluginTaskConfig;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.List;

/**
 * An interface for a plugin builder type.
 */
public interface PluginBuilder {

    @NonNull PluginGenerationTrigger.Type getType();

    @NonNull List<ObjectDef> build(@NonNull ClassElement source, @NonNull TaskConfig taskConfig);

    record TaskConfig(
        String methodName,
        String packageName,
        String namePrefix,
        String mavenPropertyPrefix,
        String gradleExtensionMethodName,
        String gradleGroup,
        boolean micronautPlugin,
        String dependency
    ) {
    }

    record ParameterConfig(
        boolean required,
        String defaultValue,
        boolean internal,
        String mavenProperty
    ) {
    }

    static @NonNull TaskConfig getTaskConfig(@NonNull ClassElement type) {
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
        AnnotationValue<PluginTaskConfig> annotation = type.getAnnotation(PluginTaskConfig.class);
        if (annotation == null) {
            return new TaskConfig(executable, type.getPackageName(), type.getSimpleName(), null, null, null, true, null);
        }
        return new TaskConfig(
            executable,
            type.getPackageName(),
            annotation.stringValue("namePrefix").orElse(type.getSimpleName()),
            annotation.stringValue("mavenPropertyPrefix").orElse(null),
            annotation.stringValue("gradleExtensionMethodName").orElse(null),
            annotation.stringValue("gradleGroup").orElse(null),
            annotation.booleanValue("micronautPlugin").orElse(true),
            annotation.stringValue("dependency").orElse(null)
        );
    }

    static @NonNull ParameterConfig getParameterConfig(@NonNull PropertyElement property) {
        AnnotationValue<PluginTaskParameter> annotation = property.getAnnotation(PluginTaskParameter.class);
        if (annotation == null) {
            return new ParameterConfig(false, null, false, null);
        }
        return new ParameterConfig(
            annotation.booleanValue("required").orElse(false),
            annotation.stringValue("defaultValue").orElse(null),
            annotation.booleanValue("internal").orElse(false),
            annotation.stringValue("mavenProperty").orElse(null)
        );
    }

}
