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
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.Type;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassDef.ClassDefBuilder;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import io.micronaut.sourcegen.model.VariableDef.Local;
import io.micronaut.sourcegen.model.VariableDef.MethodParameter;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.TASK_SUFFIX;
import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.createGradleProperty;

/**
 * A builder for {@link Type#GRADLE_EXTENSION}.
 * Creates a Gradle extension for calling a gradle task with the specification.
 */
public class GradleExtensionBuilder implements PluginBuilder {

    public static final String EXTENSION_NAME_SUFFIX = "Extension";
    public static final String DEFAULT_EXTENSION_NAME_PREFIX = "Default";
    public static final String TASK_CONFIGURATOR_SUFFIX = "TaskConfigurator";

    private static final TypeDef PROJECT_TYPE = TypeDef.of("org.gradle.api.Project");
    private static final TypeDef CONFIGURATION_TYPE = TypeDef.of("org.gradle.api.artifacts.Configuration");

    @Override
    public Type getType() {
        return Type.GRADLE_EXTENSION;
    }

    @Override
    @NonNull
    public List<ObjectDef> build(GradleTaskConfig taskConfig) {
        ClassTypeDef specificationType = ClassTypeDef.of(taskConfig.packageName() + "." + taskConfig.namePrefix() + GradleSpecificationBuilder.SPECIFICATION_NAME_SUFFIX);
        String methodName = taskConfig.gradleExtensionMethodName();
        if (methodName == null) {
            methodName = taskConfig.methodName();
        }

        return List.of(
            buildExtensionInterface(taskConfig, specificationType, methodName),
            buildDefaultExtension(taskConfig, specificationType, methodName)
        );
    }

    private ObjectDef buildExtensionInterface(GradleTaskConfig taskConfig, TypeDef specificationType, String methodName) {
        InterfaceDefBuilder builder = InterfaceDef.builder(taskConfig.packageName() + "." + taskConfig.namePrefix() + EXTENSION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Configures the " + taskConfig.namePrefix() + " execution.");

        builder.addMethod(MethodDef.builder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter("name", String.class)
            .addParameter(ParameterDef.builder("action",
                TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.Action"), specificationType)
            ).build())
            .addJavadoc("Create a task for " + methodName + "." +
                "\n@param name The unique identifier used to derive task names" +
                "\n@param spec The configurable specification"
            )
            .build()
        );
        return builder.build();
    }

    private ObjectDef buildDefaultExtension(GradleTaskConfig taskConfig, ClassTypeDef specificationType, String methodName) {
        TypeDef interfaceType = TypeDef.of(taskConfig.packageName() + "." + taskConfig.namePrefix() + EXTENSION_NAME_SUFFIX);

        ClassDefBuilder builder = ClassDef.builder(taskConfig.packageName() + "." + DEFAULT_EXTENSION_NAME_PREFIX + taskConfig.namePrefix() + EXTENSION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addSuperinterface(interfaceType)
            .addField(
                FieldDef.builder("names", TypeDef.parameterized(Set.class, String.class))
                    .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                    .initializer(ClassTypeDef.of(HashSet.class).instantiate())
                    .build()
            )
            .addField(FieldDef.builder("project", PROJECT_TYPE)
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL).build())
            .addField(FieldDef.builder("classpath", CONFIGURATION_TYPE)
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL).build());

        builder.addMethod(MethodDef.builder(MethodDef.CONSTRUCTOR)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation("javax.inject.Inject")
            .addParameter("project", PROJECT_TYPE)
            .addParameter("classpath", CONFIGURATION_TYPE)
            .build((t, params) ->
                StatementDef.multi(
                    t.field("project", PROJECT_TYPE).assign(params.get(0)),
                    t.field("classpath", CONFIGURATION_TYPE).assign(params.get(1))
                )
            ));

        ClassTypeDef actionType = TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.Action"), specificationType);
        builder.addMethod(MethodDef.builder(methodName)
            .overrides()
            .addModifiers(Modifier.PUBLIC)
            .addParameter("name", String.class)
            .addParameter(ParameterDef.builder("action", actionType).build())
            .build((t, params) -> buildExtensionMethod(t, params, taskConfig, specificationType, methodName))
        );
        builder.addMethod(buildCreateTaskMethod(taskConfig));
        builder.addMethod(MethodDef.builder("configureSpec")
            .addModifiers(Modifier.PROTECTED)
            .addParameter("spec", specificationType)
            .build((t, params) -> buildConfigureSpecMethod(taskConfig, t, params))
        );

        builder.addInnerType(buildTaskConfigurator(taskConfig, specificationType, methodName));
        return builder.build();
    }

    private ClassDef buildTaskConfigurator(GradleTaskConfig taskConfig, TypeDef specificationType, String methodName) {
        ClassTypeDef taskType = ClassTypeDef.of(taskConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        FieldDef specField = FieldDef.builder("spec", specificationType).build();
        FieldDef classpathField = FieldDef.builder("classpath", CONFIGURATION_TYPE).build();

        MethodDef execute = MethodDef.builder("execute")
            .addParameter(taskType)
            .overrides()
            .addModifiers(Modifier.PUBLIC)
            .build((t, params) -> {
                List<StatementDef> statements = new ArrayList<>();
                MethodParameter task = params.get(0);
                if (taskConfig.gradleGroup() != null) {
                    statements.add(task.invoke("setGroup", TypeDef.VOID, ExpressionDef.constant(taskConfig.gradleGroup())));
                }
                statements.add(task.invoke("getClasspath", ClassTypeDef.of("org.gradle.api.file.ConfigurableFileLocation"))
                    .invoke("from", TypeDef.VOID, t.field(classpathField))
                );
                statements.add(task.invoke("setDescription", TypeDef.VOID, ExpressionDef.constant("Configure the " + methodName)));
                for (ParameterConfig parameter: taskConfig.parameters()) {
                    String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                    TypeDef getterType = createGradleProperty(parameter);
                    if (!parameter.internal()) {
                        StatementDef convention = task
                            .invoke(getterName, getterType)
                            .invoke("convention", getterType, t.field(specField).invoke(getterName, getterType));
                        statements.add(convention);
                    }
                }
                return StatementDef.multi(statements);
            });
        return ClassDef.builder(taskConfig.namePrefix() + TASK_CONFIGURATOR_SUFFIX)
            .addModifiers(Modifier.STATIC, Modifier.PROTECTED)
            .addSuperinterface(TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.Action"), taskType))
            .addField(specField)
            .addField(classpathField)
            .addAllFieldsConstructor()
            .addMethod(execute)
            .build();
    }

    private MethodDef buildCreateTaskMethod(GradleTaskConfig taskConfig) {
        ClassTypeDef taskType = ClassTypeDef.of(taskConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        TypeDef taskProviderType = TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.tasks.TaskProvider"), TypeDef.wildcardSubtypeOf(taskType));
        TypeDef taskContainerType = TypeDef.of("org.gradle.api.tasks.TaskContainer");
        TypeDef taskConfiguratorType = ClassTypeDef.of(taskConfig.namePrefix() + TASK_CONFIGURATOR_SUFFIX);

        return MethodDef.builder("createTask")
            .returns(taskProviderType)
            .addParameter("name", String.class)
            .addParameter("configurator", taskConfiguratorType)
            .build((t, params) ->
                    t.field("project", PROJECT_TYPE)
                    .invoke("getTasks", taskContainerType)
                    .invoke("register", taskProviderType,
                        params.get(0),
                        taskType.getStaticField("class", TypeDef.CLASS),
                        params.get(1)
                    ).returning()
            );
    }

    private StatementDef buildConfigureSpecMethod(GradleTaskConfig taskConfig, VariableDef t, List<VariableDef.MethodParameter> params) {
        List<StatementDef> statements = new ArrayList<>();
        for (ParameterConfig parameter: taskConfig.parameters()) {
            String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
            TypeDef getterType = createGradleProperty(parameter);
            if (parameter.defaultValue() != null && !parameter.internal()) {
                ClassElement type = parameter.source().getType();
                StatementDef convention = params.get(0)
                    .invoke(getterName, getterType)
                    .invoke("convention", getterType, ExpressionDef.constant(type, TypeDef.of(type), parameter.defaultValue()));
                statements.add(convention);
            }
        }
        return StatementDef.multi(statements);
    }

    private StatementDef buildExtensionMethod(VariableDef t, List<VariableDef.MethodParameter> params, GradleTaskConfig taskConfig, ClassTypeDef specificationType, String methodName) {
        StatementDef ifStatement = new StatementDef.If(
            t.field("names", TypeDef.of(String.class)).invoke("add", TypeDef.of(boolean.class), params.get(0)).isFalse(),
            new StatementDef.Throw(ClassTypeDef.of("org.gradle.api.GradleException")
                .instantiate(ExpressionDef.constant("An " + methodName + " definition with name '").math("+", params.get(0)).math("+", ExpressionDef.constant("' was already created")))
            )
        );
        TypeDef objectFactoryType = TypeDef.of("org.gradle.api.model.ObjectFactory");
        Local spec = new Local("spec", specificationType);
        StatementDef specCreation = new StatementDef.DefineAndAssign(
            spec,
            t.field("project", PROJECT_TYPE)
                .invoke("getObjects", objectFactoryType)
                .invoke("newInstance", specificationType, specificationType.getStaticField("class", TypeDef.CLASS))
        );
        StatementDef configureSpec = t.invoke("configureSpec", TypeDef.VOID, spec);
        StatementDef actionCall = params.get(1).invoke("execute", TypeDef.VOID, spec);

        ClassTypeDef taskType = ClassTypeDef.of(taskConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        TypeDef taskProviderType = TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.tasks.TaskProvider"), TypeDef.wildcardSubtypeOf(taskType));
        ExpressionDef taskConfigurator = ClassTypeDef.of(taskConfig.namePrefix() + TASK_CONFIGURATOR_SUFFIX)
           .instantiate(spec, t.field("classpath", CONFIGURATION_TYPE));
        Local task = new Local("task", taskProviderType);
        StatementDef taskCreation = new StatementDef.DefineAndAssign(
            task,
            t.invoke("createTask", taskProviderType, params.get(0), taskConfigurator)
        );
        // TODO source sets
        return StatementDef.multi(
            ifStatement,
            specCreation,
            configureSpec,
            actionCall,
            taskCreation
        );
    }

}
