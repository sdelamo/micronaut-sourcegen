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
package io.micronaut.sourcegen.custom.visitor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.custom.example.GenerateMethodInvocation;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;

import javax.lang.model.element.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Internal
public final class GenerateMethodInvocationVisitor implements TypeElementVisitor<GenerateMethodInvocation, Object> {

    private static final java.lang.reflect.Method LOCK_METHOD = ReflectionUtils.getRequiredInternalMethod(
        Lock.class,
        "lock"
    );

    private static final java.lang.reflect.Method UNLOCK_METHOD = ReflectionUtils.getRequiredInternalMethod(
        Lock.class,
        "unlock"
    );

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
        if (sourceGenerator == null) {
            return;
        }

        ClassElement myRepository = context.getRequiredClassElement("io.micronaut.sourcegen.example.MyRepository", context.getElementAnnotationMetadataFactory());

        ClassTypeDef repositoryType = ClassTypeDef.of(myRepository);
        ClassDef classDef = ClassDef.builder("io.micronaut.sourcegen.example.MethodInvoker")

            .addMethod(MethodDef.builder("invokeDefaultMethod")
                .addParameter(repositoryType)
                .addParameters(String.class, Integer.class, int.class)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("defaultMethod", TypeDef.STRING, methodParameters.subList(1, methodParameters.size()))
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeInterfaceMethod")
                .addParameter(repositoryType)
                .addParameters(String.class, Integer.class, int.class)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("interfaceMethod", TypeDef.STRING, methodParameters.subList(1, methodParameters.size()))
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeInterfaceMethodReturnsInt")
                .addParameter(repositoryType)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("interfaceMethodReturnsInt", TypeDef.Primitive.INT)
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeInterfaceMethodReturnsDouble")
                .addParameter(repositoryType)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("interfaceMethodReturnsDouble", TypeDef.Primitive.DOUBLE)
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeInterfaceMethodReturnsLong")
                .addParameter(repositoryType)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("interfaceMethodReturnsLong", TypeDef.Primitive.LONG)
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeStaticMethod")
                .addParameters(String.class, Integer.class, int.class)
                .buildStatic(methodParameters -> repositoryType
                    .invokeStatic("staticMethod", TypeDef.STRING, methodParameters)
                    .returning())
            )

            .addMethod(MethodDef.builder("invokeDefaultMethodIgnoreResult")
                .addParameter(repositoryType)
                .addParameters(String.class, Integer.class, int.class)
                .addStaticStatement(methodParameters -> methodParameters.get(0)
                    .invoke("defaultMethod", TypeDef.STRING, methodParameters.subList(1, methodParameters.size())))
                .buildStatic(methodParameters -> ExpressionDef.constant("Ignored").returning())
            )

            .addMethod(MethodDef.builder("invokeInterfaceMethodIgnoreResult")
                .addParameter(repositoryType)
                .addParameters(String.class, Integer.class, int.class)
                .addStaticStatement(methodParameters -> methodParameters.get(0)
                    .invoke("interfaceMethod", TypeDef.STRING, methodParameters.subList(1, methodParameters.size())))
                .buildStatic(methodParameters -> ExpressionDef.constant("Ignored").returning())
            )

            .addMethod(MethodDef.builder("invokeStaticMethodIgnoreResult")
                .addParameters(String.class, Integer.class, int.class)
                .addStaticStatement(methodParameters -> repositoryType
                    .invokeStatic("staticMethod", TypeDef.STRING, methodParameters))
                .buildStatic(methodParameters -> ExpressionDef.constant("Ignored").returning())
            )

            .addMethod(MethodDef.builder("invokeTryFinallyReadLock")
                .addParameters(ReentrantReadWriteLock.class)
                .returns(Object.class)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("readLock", TypeDef.of(Lock.class))
                    .newLocal("readLock", readLock -> StatementDef.multi(
                        readLock.invoke("lock", TypeDef.VOID),
                        ExpressionDef.constant(123).returning().doTry()
                            .doFinally(readLock.invoke("unlock", TypeDef.VOID))
                    )))
            )

            .addMethod(MethodDef.builder("invokeTryFinallyWriteLock")
                .addParameters(ReentrantReadWriteLock.class)
                .returns(Object.class)
                .buildStatic(methodParameters -> methodParameters.get(0)
                    .invoke("writeLock", TypeDef.of(Lock.class))
                    .newLocal("writeLock", writeLock -> StatementDef.multi(
                        writeLock.invoke("lock", TypeDef.VOID),
                        ExpressionDef.constant(123).returning().doTry()
                            .doFinally(writeLock.invoke("unlock", TypeDef.VOID))
                    )))
            )

            .addMethod(MethodDef.builder("invokeTryFinally")
                .addParameters(AtomicInteger.class)
                .returns(Object.class)
                .buildStatic(methodParameters -> ExpressionDef.constant("Ignored")
                    .returning()
                    .doTry()
                    .doFinally(methodParameters.get(0).invoke("getAndIncrement", TypeDef.Primitive.INT)))
            )

            .build();

        sourceGenerator.write(classDef, context, element);

        FieldDef targetField = FieldDef.builder("target", TypeDef.OBJECT).addModifiers(Modifier.PRIVATE).build();
        FieldDef lockField = FieldDef.builder("lock", ClassTypeDef.of(ReentrantReadWriteLock.class))
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(ClassTypeDef.of(ReentrantReadWriteLock.class).instantiate())
            .build();
        FieldDef writeLockField = FieldDef.builder("writeLock", ClassTypeDef.of(Lock.class))
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(new VariableDef.This().field(lockField).invoke("writeLock", TypeDef.of(Lock.class)))
            .build();

        ClassDef swapper = ClassDef.builder("io.micronaut.sourcegen.example.Swapper")
            .addField(targetField)
            .addField(lockField)
            .addField(writeLockField)
            .addMethod(MethodDef.builder("getTarget").build((aThis, methodParameters) -> aThis.field(targetField).returning()))
            .addMethod(MethodDef.builder("swap")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> {
                    VariableDef.Field lock = aThis.field(writeLockField);
                    return StatementDef.multi(
                        lock.invoke(LOCK_METHOD),
                        StatementDef.doTry(
                            aThis.field(targetField).newLocal("target", targetVar -> StatementDef.multi(
                                aThis.field(targetField).assign(methodParameters.get(0)),
                                targetVar.returning()
                            ))
                        ).doFinally(lock.invoke(UNLOCK_METHOD))
                    );
                }))
            .addMethod(MethodDef.builder("swap2")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class, AtomicInteger.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    methodParameters.get(1).invoke("getAndIncrement", TypeDef.Primitive.INT),
                    StatementDef.doTry(
                        aThis.field(targetField).newLocal("target", targetVar -> StatementDef.multi(
                            aThis.field(targetField).assign(methodParameters.get(0)),
                            targetVar.returning()
                        ))
                    ).doFinally(methodParameters.get(1).invoke("getAndDecrement", TypeDef.Primitive.INT))
                )))
            .addMethod(MethodDef.builder("swap3")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class, AtomicInteger.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    methodParameters.get(1).invoke("getAndIncrement", TypeDef.Primitive.INT),
                    StatementDef.doTry(
                        StatementDef.multi(
                            ClassTypeDef.of(IllegalStateException.class).instantiate().doThrow(),
                            methodParameters.get(1).invoke("getAndIncrement", TypeDef.Primitive.INT),
                            aThis.field(targetField).returning()
                        )
                    ).doFinally(methodParameters.get(1).invoke("getAndDecrement", TypeDef.Primitive.INT))
                )))
            .addMethod(MethodDef.builder("swap4")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class, AtomicInteger.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    methodParameters.get(1).invoke("getAndIncrement", TypeDef.Primitive.INT),
                    StatementDef.doTry(
                            StatementDef.multi(
                                ClassTypeDef.of(IllegalStateException.class).instantiate(ExpressionDef.constant("Bam")).doThrow(),
                                methodParameters.get(1).invoke("getAndIncrement", TypeDef.Primitive.INT),
                                aThis.field(targetField).returning()
                            )
                        ).doCatch(Throwable.class, exceptionVar
                            -> exceptionVar.invoke("getMessage", TypeDef.of(String.class))
                            .returning())
                        .doFinally(methodParameters.get(1).invoke("getAndDecrement", TypeDef.Primitive.INT))
                )))
            .addMethod(MethodDef.builder("swap5")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    new StatementDef.Synchronized(aThis,
                        aThis.field(targetField).newLocal("target", targetVar -> StatementDef.multi(
                            aThis.field(targetField).assign(methodParameters.get(0)),
                            targetVar.returning()
                        ))
                    )
                )))
            .addMethod(MethodDef.builder("swap6")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(Object.class)
                .returns(Object.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    new StatementDef.Synchronized(aThis,
                        ClassTypeDef.of(IllegalStateException.class).instantiate(ExpressionDef.constant("Bam")).doThrow()
                    )
                )))
            .build();

        sourceGenerator.write(swapper, context, element);
    }

}
