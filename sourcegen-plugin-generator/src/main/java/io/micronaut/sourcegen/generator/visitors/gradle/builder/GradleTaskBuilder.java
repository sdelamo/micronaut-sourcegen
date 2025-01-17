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
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.Type;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassDef.ClassDefBuilder;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.MethodDef.MethodDefBuilder;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A builder for {@link GenerateGradlePlugin.Type#GRADLE_TASK}.
 * Creates a task, work action and work action parameters given a plugin task configuration.
 */
public class GradleTaskBuilder implements GradleTypeBuilder {

    public static final String TASK_SUFFIX = "Task";

    @Override
    public Type getType() {
        return Type.GRADLE_TASK;
    }

    @Override
    @NonNull
    public List<ObjectDef> build(GradlePluginConfig pluginConfig) {
        List<ObjectDef> objects = new ArrayList<>();
        for (GradleTaskConfig taskConfig: pluginConfig.tasks()) {
            objects.addAll(buildTask(pluginConfig.packageName(), taskConfig));
        }
        return objects;
    }

    private List<ObjectDef> buildTask(String packageName, GradleTaskConfig taskConfig) {
        String taskType = packageName + "." + taskConfig.namePrefix() + TASK_SUFFIX;
        ClassDefBuilder builder = ClassDef.builder(taskType)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .superclass(ClassTypeDef.of("org.gradle.api.DefaultTask"))
            .addAnnotation("org.gradle.api.tasks.CacheableTask");
        builder.addInnerType(createWorkAction(taskConfig));
        builder.addInnerType(createWorkActionParameters(taskConfig));
        builder.addInnerType(createWorkActionParameterConfigurator(TypeDef.of(taskType), taskConfig));
        builder.addInnerType(createClasspathConfigurator(TypeDef.of(taskType), taskConfig));

        for (ParameterConfig parameter: taskConfig.parameters()) {
            MethodDefBuilder propBuilder = MethodDef
                .builder("get" + NameUtils.capitalize(parameter.source().getName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(createGradleProperty(parameter));
            if (parameter.output()) {
                if (parameter.source().getType().isAssignable(File.class)) {
                    if (parameter.directory()) {
                        propBuilder.addAnnotation(AnnotationDef.builder(ClassTypeDef.of("org.gradle.api.tasks.OutputDirectory")).build());
                    } else {
                        propBuilder.addAnnotation(AnnotationDef.builder(ClassTypeDef.of("org.gradle.api.tasks.OutputFile")).build());
                    }
                }
            } else {
                propBuilder.addAnnotation("org.gradle.api.tasks.Input");
                if (parameter.source().getType().isAssignable(File.class)) {
                    if (parameter.directory()) {
                        propBuilder.addAnnotation(AnnotationDef.builder(ClassTypeDef.of("org.gradle.api.tasks.InputDirectory")).build());
                    } else {
                        propBuilder.addAnnotation(AnnotationDef.builder(ClassTypeDef.of("org.gradle.api.tasks.InputFile")).build());
                    }
                    propBuilder.addAnnotation(AnnotationDef.builder(ClassTypeDef.of("org.gradle.api.tasks.PathSensitive"))
                        .addMember("value", ClassTypeDef.of("org.gradle.api.tasks.PathSensitivity")
                            .getStaticField("NONE", TypeDef.of("org.gradle.api.tasks.PathSensitivity"))
                        ).build()
                    );
                }
            }

            if (!parameter.required()) {
                propBuilder.addAnnotation("org.gradle.api.tasks.Optional");
            }
            builder.addMethod(propBuilder.build());
        }

        TypeDef classpathType = TypeDef.of("org.gradle.api.file.ConfigurableFileCollection");
        builder.addMethod(MethodDef.builder("getClasspath")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(classpathType)
            .addAnnotation("org.gradle.api.tasks.Classpath")
            .build()
        );

        TypeDef workerExecutorType = TypeDef.of("org.gradle.workers.WorkerExecutor");
        builder.addMethod(MethodDef.builder("getWorkerExecutor")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(workerExecutorType)
            .addAnnotation("javax.inject.Inject")
            .build()
        );

        builder.addMethod(MethodDef.builder("execute")
            .returns(TypeDef.VOID)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation("org.gradle.api.tasks.TaskAction")
            .build((t, params) ->
                t.invoke("getWorkerExecutor", workerExecutorType)
                    .invoke("classLoaderIsolation",
                        workerExecutorType,
                        ClassTypeDef.of(taskConfig.namePrefix() + "ClasspathConfigurator").instantiate(t)
                    )
                    .invoke("submit", TypeDef.VOID,
                        ClassTypeDef.of(taskConfig.namePrefix() + "WorkAction").getStaticField("class", TypeDef.CLASS),
                        ClassTypeDef.of(taskConfig.namePrefix() + "WorkActionParameterConfigurator").instantiate(t)
                    )
            )
        );

        return List.of(builder.build());
    }

    private ClassDef createWorkActionParameterConfigurator(TypeDef taskType, GradleTaskConfig taskConfig) {
        TypeDef parametersType = TypeDef.of(taskConfig.namePrefix() + "WorkActionParameters");
        FieldDef taskField = FieldDef.builder("task").ofType(taskType).build();
        return ClassDef.builder(taskConfig.namePrefix() + "WorkActionParameterConfigurator")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterface(TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.Action"),
                parametersType
            ))
            .addField(taskField)
            .addAllFieldsConstructor(Modifier.PUBLIC)
            .addMethod(MethodDef.builder("execute")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeDef.VOID)
                .overrides()
                .addParameter(ParameterDef.of("params", parametersType))
                .build((t, params) -> {
                    List<StatementDef> statements = new ArrayList<>();
                    for (ParameterConfig parameter: taskConfig.parameters()) {
                        String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                        TypeDef getterType = createGradleProperty(parameter);
                        ExpressionDef def = t.field(taskField).invoke(getterName, getterType);
                        if (!parameter.required()) {
                            if (parameter.defaultValue() != null) {
                                ClassElement type = parameter.source().getType();
                                def = def.invoke(
                                    "orElse",
                                    TypeDef.of(parameter.source().getType()),
                                    ExpressionDef.constant(type, TypeDef.of(type), parameter.defaultValue())
                                );
                            } else {
                                def = def.invoke("getOrNull", TypeDef.of(parameter.source().getType()));
                            }
                        }
                        statements.add(params.get(0)
                            .invoke(getterName, getterType)
                            .invoke("set", TypeDef.VOID, def)
                        );
                    }
                    return StatementDef.multi(statements);
                })
            )
            .build();
    }

    private ClassDef createClasspathConfigurator(TypeDef taskType, GradleTaskConfig taskConfig) {
        FieldDef taskField = FieldDef.builder("task").ofType(taskType).build();
        TypeDef specType = TypeDef.of("org.gradle.workers.ClassLoaderWorkerSpec");
        TypeDef classpathType = TypeDef.of("org.gradle.api.file.ConfigurableFileCollection");
        return ClassDef.builder(taskConfig.namePrefix() + "ClasspathConfigurator")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterface(TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.Action"),
                specType
            ))
            .addField(taskField)
            .addAllFieldsConstructor(Modifier.PUBLIC)
            .addMethod(MethodDef.builder("execute")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeDef.VOID)
                .overrides()
                .addParameter(ParameterDef.of("spec", specType))
                .build((t, params) ->
                    params.get(0)
                        .invoke("getClasspath", classpathType)
                        .invoke("from", TypeDef.VOID, t.field(taskField).invoke("getClasspath", classpathType))
                )
            )
            .build();
    }

    private InterfaceDef createWorkActionParameters(GradleTaskConfig taskConfig) {
        InterfaceDefBuilder builder = InterfaceDef.builder(taskConfig.namePrefix() + "WorkActionParameters")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassTypeDef.of("org.gradle.workers.WorkParameters"));
        for (ParameterConfig parameter: taskConfig.parameters()) {
            MethodDefBuilder propBuilder = MethodDef
                .builder("get" + NameUtils.capitalize(parameter.source().getName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(createGradleProperty(parameter));
            builder.addMethod(propBuilder.build());
        }
        return builder.build();
    }

    private ClassDef createWorkAction(GradleTaskConfig taskConfig) {
        ClassTypeDef parametersType = ClassTypeDef.of(taskConfig.namePrefix() + "WorkActionParameters");
        List<ExpressionDef> params = new ArrayList<>();
        for (ParameterConfig parameter: taskConfig.parameters()) {
            ExpressionDef expression = new VariableDef.Local("parameters", parametersType)
                .invoke("get" + NameUtils.capitalize(parameter.source().getName()), createGradleProperty(parameter))
                .invoke("get", TypeDef.of(parameter.source().getType()));
            if (parameter.source().getType().isAssignable(File.class)) {
                expression = expression.invoke("getAsFile", TypeDef.of(File.class));
            }
            params.add(expression);
        }
        MethodDef executeMethod = MethodDef
            .builder("execute")
            .returns(TypeDef.VOID)
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addStatement(
                new VariableDef.This()
                    .invoke("getParameters", parametersType)
                    .newLocal("parameters")
            )
            .addStatement(
                ClassTypeDef.of(taskConfig.source()).instantiate(params).invoke(taskConfig.methodName(), TypeDef.VOID)
            )
            .build();
        return ClassDef.builder(taskConfig.namePrefix() + "WorkAction")
            .addSuperinterface(TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.workers.WorkAction"),
                parametersType
            ))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
            .addMethod(executeMethod)
            .build();
    }

    static TypeDef createGradleProperty(ParameterConfig parameter) {
        ClassElement type = parameter.source().getType();
        if (type.isAssignable(File.class)) {
            if (parameter.directory()) {
                return ClassTypeDef.of("org.gradle.api.file.DirectoryProperty");
            }
            return ClassTypeDef.of("org.gradle.api.file.RegularFileProperty");
        }
        if (type.isAssignable(Map.class)) {
            Map<String, ClassElement> typeArgs = type.getGenericType().getTypeArguments();
            return TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.provider.MapProperty"),
                ClassTypeDef.of(typeArgs.get("K")),
                ClassTypeDef.of(typeArgs.get("V"))
            );
        } else if (type.isAssignable(List.class)) {
            Map<String, ClassElement> typeArgs = type.getGenericType().getTypeArguments();
            return TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.provider.ListProperty"),
                ClassTypeDef.of(typeArgs.get("E"))
            );
        } else if (type.isAssignable(Set.class)) {
            Map<String, ClassElement> typeArgs = type.getGenericType().getTypeArguments();
            return TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.provider.SetProperty"),
                ClassTypeDef.of(typeArgs.get("E"))
            );
        } else {
            return TypeDef.parameterized(
                ClassTypeDef.of("org.gradle.api.provider.Property"),
                ClassTypeDef.of(parameter.source().getGenericType())
            );
        }
    }

}
