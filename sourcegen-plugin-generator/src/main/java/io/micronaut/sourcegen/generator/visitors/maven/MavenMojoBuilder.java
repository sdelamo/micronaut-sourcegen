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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.sourcegen.generator.visitors.maven.MavenPluginUtils.MavenTaskConfig;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.AnnotationDef.AnnotationDefBuilder;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassDef.ClassDefBuilder;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A builder for Maven Mojos.
 */
@Internal
public class MavenMojoBuilder {

    public static final String MOJO_SUFFIX = "Mojo";

    /**
     * Method for building the Maven mojo.
     *
     * @param taskConfig The config
     * @return The class
     */
    public ClassDef build(MavenTaskConfig taskConfig) {
        String mojoName = taskConfig.packageName() + "." + taskConfig.namePrefix() + MOJO_SUFFIX;
        ClassDefBuilder builder = ClassDef.builder(mojoName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        if (taskConfig.micronautPlugin()) {
            builder.superclass(ClassTypeDef.of("io.micronaut.maven.AbstractMicronautMojo"));
        } else {
            builder.superclass(ClassTypeDef.of("org.apache.maven.plugin.AbstractMojo"));
        }

        for (ParameterConfig parameter : taskConfig.parameters()) {
            addParameter(taskConfig, parameter, builder);
        }

        builder.addMethod(MethodDef.builder("isEnabled")
            .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
            .returns(TypeDef.of(boolean.class))
            .addJavadoc("Determines if this mojo must be executed.\n@return true if the mojo is enabled")
            .build()
        );
        builder.addMethod(createExecuteMethod(taskConfig));
        builder.addJavadoc(taskConfig.taskJavadoc());

        return builder.build();
    }

    private void addParameter(MavenTaskConfig taskConfig, ParameterConfig parameter, ClassDefBuilder builder) {
        if (parameter.internal() || parameter.output()) {
            builder.addMethod(MethodDef
                .builder("get" + NameUtils.capitalize(parameter.source().getName()))
                .returns(TypeDef.of(parameter.source().getType()))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addJavadoc(parameter.javadoc())
                .build()
            );
        } else {
            AnnotationDefBuilder ann = AnnotationDef.builder(ClassTypeDef.of("org.apache.maven.plugins.annotations.Parameter"));
            if (parameter.defaultValue() != null) {
                ann.addMember("defaultValue", parameter.defaultValue());
            }
            if (parameter.required()) {
                ann.addMember("required", true);
            }
            if (parameter.globalProperty() != null) {
                ann.addMember("property",  taskConfig.mavenPropertyPrefix()
                    + "." + MavenPluginUtils.toDotSeparated(parameter.globalProperty()));
            }
            FieldDef field = FieldDef.builder(parameter.source().getName())
                .ofType(TypeDef.of(parameter.source().getType()))
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(ann.build())
                .addJavadoc(parameter.javadoc())
                .build();
            builder.addField(field);
        }
    }

    private MethodDef createExecuteMethod(MavenTaskConfig taskConfig) {
        return MethodDef.builder("execute")
            .overrides()
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(taskConfig.methodJavadoc())
            .build((t, params) -> {
                StatementDef isEnabled = t.invoke("isEnabled", TypeDef.of(boolean.class))
                    .ifFalse(StatementDef.multi(
                        t.invoke("getLog", ClassTypeDef.of("org.apache.maven.plugin.logging.Log"))
                            .invoke("debug", TypeDef.VOID, ExpressionDef.constant(taskConfig.namePrefix() + MOJO_SUFFIX + " is disabled")),
                        new StatementDef.Return(null)
                    ));
                return StatementDef.multi(
                    isEnabled,
                    runTask(taskConfig, t)
                );
            });
    }

    private StatementDef runTask(MavenTaskConfig taskConfig, VariableDef.This t) {
        List<ExpressionDef> params = new ArrayList<>();
        for (ParameterConfig parameter: taskConfig.parameters()) {
            if (parameter.internal() || parameter.output()) {
                String getter = "get" + NameUtils.capitalize(parameter.source().getName());
                params.add(t.invoke(getter, TypeDef.of(parameter.source().getType())));
            } else {
                params.add(t.field(parameter.source().getName(), TypeDef.of(parameter.source().getType())));
            }
        }
        return ClassTypeDef.of(taskConfig.source()).instantiate(params)
            .invoke(taskConfig.methodName(), TypeDef.VOID);
    }

}
