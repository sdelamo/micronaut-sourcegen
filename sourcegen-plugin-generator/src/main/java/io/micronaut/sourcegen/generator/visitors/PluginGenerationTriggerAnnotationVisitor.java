/*
 * Copyright 2017-2023 original authors
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
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.PluginGenerationTrigger;
import io.micronaut.sourcegen.annotations.PluginGenerationTrigger.Type;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.generator.visitors.builder.gradle.GradleExtensionBuilder;
import io.micronaut.sourcegen.generator.visitors.builder.gradle.GradlePluginBuilder;
import io.micronaut.sourcegen.generator.visitors.builder.gradle.GradleSpecificationBuilder;
import io.micronaut.sourcegen.generator.visitors.builder.gradle.GradleTaskBuilder;
import io.micronaut.sourcegen.generator.visitors.builder.PluginBuilder;
import io.micronaut.sourcegen.generator.visitors.builder.PluginBuilder.TaskConfig;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The visitor that is generation a builder.
 *
 * @author Andriy Dmytruk
 * @since 1.5.x
 */
@Internal
public final class PluginGenerationTriggerAnnotationVisitor implements TypeElementVisitor<PluginGenerationTrigger, Object> {

    private final Set<String> processed = new HashSet<>();
    private static final List<PluginBuilder> builders = List.of(
        new GradleTaskBuilder(),
        new GradleExtensionBuilder(),
        new GradleSpecificationBuilder(),
        new GradlePluginBuilder()
    );

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void start(VisitorContext visitorContext) {
        processed.clear();
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(PluginGenerationTrigger.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        context.info("Creating plugin classes");
        if (processed.contains(element.getName())) {
            return;
        }
        AnnotationValue<PluginGenerationTrigger> annotation = element.getAnnotation(PluginGenerationTrigger.class);
        if (annotation == null) {
            return;
        }
        ClassElement source = element.stringValue(PluginGenerationTrigger.class, "source")
            .flatMap(context::getClassElement).orElse(null);
        if (source == null) {
            throw new ProcessingException(element, "Could not load source type defined in @PluginGenerationTrigger");
        }
        PluginGenerationTrigger.Type[] types = annotation.getRequiredValue("types", PluginGenerationTrigger.Type[].class);

        List<ObjectDef> definitions = new ArrayList<>();
        for (Type type: types) {
            TaskConfig taskConfig = PluginBuilder.getTaskConfig(source);
            List<ObjectDef> typeDefinitions = new ArrayList<>();
            for (PluginBuilder pluginBuilder : builders) {
                if (pluginBuilder.getType().equals(type)) {
                    typeDefinitions = pluginBuilder.build(source, taskConfig);
                }
            }
            if (typeDefinitions == null) {
                throw new ProcessingException(source, "Building plugin sources of type " + type + " not supported!");
            }
            definitions.addAll(typeDefinitions);
        }

        try {
            SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
            if (sourceGenerator == null) {
                throw new ProcessingException(element, "Could not find SourceGenerator for language " + context.getLanguage());
            }
            processed.add(element.getName());
            for (ObjectDef definition : definitions) {
                sourceGenerator.write(definition, context, element);
            }
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            SourceGenerators.handleFatalException(element, PluginGenerationTrigger.class, e,
                (exception -> {
                    processed.remove(element.getName());
                    throw exception;
                })
            );
        }
    }

}
