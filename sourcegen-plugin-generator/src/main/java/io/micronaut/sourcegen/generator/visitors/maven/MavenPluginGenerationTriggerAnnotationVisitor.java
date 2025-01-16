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
package io.micronaut.sourcegen.generator.visitors.maven;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.GenerateMavenMojo;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.generator.visitors.maven.MavenPluginUtils.MavenTaskConfig;
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
public final class MavenPluginGenerationTriggerAnnotationVisitor implements TypeElementVisitor<GenerateMavenMojo, Object> {

    private final Set<String> processed = new HashSet<>();

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
        return Set.of(GenerateMavenMojo.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        context.info("Creating plugin classes");
        if (processed.contains(element.getName())) {
            return;
        }
        try {
            AnnotationValue<GenerateMavenMojo> annotation = element.getAnnotation(GenerateMavenMojo.class);
            if (annotation == null) {
                return;
            }
            ClassElement source = element.stringValue(GenerateMavenMojo.class, "source")
                .flatMap(context::getClassElement).orElse(null);
            if (source == null) {
                throw new ProcessingException(element, "Could not load source type defined in @PluginGenerationTrigger");
            }

            MavenTaskConfig taskConfig = MavenPluginUtils.getTaskConfig(source, annotation);
            List<ObjectDef> definitions = new ArrayList<>();
            definitions.add(new MavenMojoBuilder().build(taskConfig));

            SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
            if (sourceGenerator == null) {
                throw new ProcessingException(element, "Could not find SourceGenerator for language " + context.getLanguage());
            }
            processed.add(element.getName());
            for (ObjectDef definition : definitions) {
                sourceGenerator.write(definition, context, element);
            }
        } catch (ProcessingException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            SourceGenerators.handleFatalException(element, GenerateMavenMojo.class, e,
                (exception -> {
                    processed.remove(element.getName());
                    throw exception;
                })
            );
        }
    }

}
