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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A builder for {@link io.micronaut.sourcegen.annotations.PluginGenerationTrigger.Type#GRADLE_TASK}.
 * Creates a task, work action and work action parameters given a plugin task configuration.
 */
public class GradleTaskBuilder implements PluginBuilder {

    public static final String TASK_SUFFIX = "Task";

    @Override
    public Type getType() {
        return Type.GRADLE_TASK;
    }

    @Override
    @NonNull
    public List<ObjectDef> build(ClassElement source, TaskConfig taskConfig) {
        String taskType = taskConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX;
        ClassDefBuilder builder = ClassDef.builder(taskType)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .superclass(ClassTypeDef.of("org.gradle.api.DefaultTask"))
            .addAnnotation("org.gradle.api.tasks.CacheableTask");
        builder.addInnerType(createWorkAction(source, taskConfig));
        builder.addInnerType(createWorkActionParameters(source, taskConfig));
        builder.addInnerType(createWorkActionParameterConfigurator(source, TypeDef.of(taskType), taskConfig));
        builder.addInnerType(createClasspathConfigurator(TypeDef.of(taskType), taskConfig));

        for (PropertyElement property: source.getBeanProperties()) {
            ParameterConfig parameterConfig = PluginBuilder.getParameterConfig(property);

            if (parameterConfig.internal()) {
                MethodDefBuilder propBuilder = MethodDef
                    .builder("get" + NameUtils.capitalize(property.getName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeDef.parameterized(
                        ClassTypeDef.of("org.gradle.api.provider.Provider"),
                        TypeDef.of(property)
                    ));
                builder.addMethod(propBuilder.build());
            } else {
                MethodDefBuilder propBuilder = MethodDef
                    .builder("get" + NameUtils.capitalize(property.getName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(createGradleProperty(property))
                    .addAnnotation("org.gradle.api.tasks.Input");
                if (!parameterConfig.required()) {
                    propBuilder.addAnnotation("org.gradle.api.tasks.Optional");
                }
                builder.addMethod(propBuilder.build());
            }
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

    private ClassDef createWorkActionParameterConfigurator(ClassElement source, TypeDef taskType, TaskConfig taskConfig) {
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
                    for (PropertyElement property: source.getBeanProperties()) {
                        ParameterConfig parameterConfig = PluginBuilder.getParameterConfig(property);
                        String getterName = "get" + NameUtils.capitalize(property.getName());
                        TypeDef getterType = createGradleProperty(property);
                        ExpressionDef def = t.field(taskField).invoke(getterName, getterType);
                        if (!parameterConfig.required()) {
                            if (parameterConfig.defaultValue() != null) {
                                ClassElement type = property.getType();
                                def = def.invoke(
                                    "orElse",
                                    TypeDef.of(property.getType()),
                                    ExpressionDef.constant(type, TypeDef.of(type), parameterConfig.defaultValue())
                                );
                            } else {
                                def = def.invoke("getOrNull", TypeDef.of(property.getType()));
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

    private ClassDef createClasspathConfigurator(TypeDef taskType, TaskConfig taskConfig) {
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

    private InterfaceDef createWorkActionParameters(ClassElement source, TaskConfig taskConfig) {
        InterfaceDefBuilder builder = InterfaceDef.builder(taskConfig.namePrefix() + "WorkActionParameters")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassTypeDef.of("org.gradle.workers.WorkParameters"));
        for (PropertyElement property: source.getBeanProperties()) {
            MethodDefBuilder propBuilder = MethodDef
                .builder("get" + NameUtils.capitalize(property.getName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(createGradleProperty(property));
            builder.addMethod(propBuilder.build());
        }
        return builder.build();
    }

    private ClassDef createWorkAction(ClassElement source, TaskConfig taskConfig) {
        ClassTypeDef parametersType = ClassTypeDef.of(taskConfig.namePrefix() + "WorkActionParameters");
        List<ExpressionDef> params = new ArrayList<>();
        for (PropertyElement property: source.getBeanProperties()) {
            params.add(new VariableDef.Local("parameters", parametersType)
                .invoke("get" + NameUtils.capitalize(property.getName()), createGradleProperty(property))
                .invoke("get", TypeDef.of(property.getType()))
            );
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
                ClassTypeDef.of(source).instantiate(params).invoke(taskConfig.methodName(), TypeDef.VOID)
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

    static TypeDef createGradleProperty(PropertyElement source) {
        ClassElement type = source.getType();
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
                ClassTypeDef.of(source.getGenericType())
            );
        }
    }

}
