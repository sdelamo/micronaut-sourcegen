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
package io.micronaut.sourcegen.generator.visitors.builder.gradle;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.sourcegen.annotations.PluginGenerationTrigger.Type;
import io.micronaut.sourcegen.generator.visitors.builder.PluginBuilder;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.MethodDef.MethodDefBuilder;
import io.micronaut.sourcegen.model.ObjectDef;

import javax.lang.model.element.Modifier;
import java.util.List;

import static io.micronaut.sourcegen.generator.visitors.builder.gradle.GradleTaskBuilder.createGradleProperty;

/**
 * A builder for {@link Type#GRADLE_SPECIFICATION}.
 * Creates a Gradle specification for configuring a gradle task.
 */
public class GradleSpecificationBuilder implements PluginBuilder {

    public static final String SPECIFICATION_NAME_SUFFIX = "Spec";

    @Override
    public Type getType() {
        return Type.GRADLE_SPECIFICATION;
    }

    @Override
    @NonNull
    public List<ObjectDef> build(ClassElement source, TaskConfig taskConfig) {
        InterfaceDefBuilder builder = InterfaceDef.builder( taskConfig.packageName() + "." + taskConfig.namePrefix() + SPECIFICATION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC);
        for (PropertyElement property: source.getBeanProperties()) {
            if (PluginBuilder.getParameterConfig(property).internal()) {
                continue;
            }
            MethodDefBuilder propBuilder = MethodDef
                .builder("get" + NameUtils.capitalize(property.getName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(createGradleProperty(property));
            builder.addMethod(propBuilder.build());
        }
        return List.of(builder.build());
    }

}
