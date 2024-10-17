/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.sourcegen.bytecode;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.JavaIdioms;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.PropertyDef;
import io.micronaut.sourcegen.model.RecordDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;
import org.objectweb.asm.util.CheckClassAdapter;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_RECORD;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.V17;

/**
 * Generates the classes directly by writing the bytecode.
 *
 * @author Denis Stepanov
 * @since 1.5
 */
public final class ByteCodeWriter {

    private final boolean checkClass;
    private final boolean visitMaxs;

    public ByteCodeWriter() {
        this(true, true);
    }

    public ByteCodeWriter(boolean checkClass, boolean visitMaxs) {
        this.checkClass = checkClass;
        this.visitMaxs = visitMaxs;
    }

    private ClassWriter generateClassBytes(ObjectDef objectDef) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = classWriter;
        if (checkClass) {
            classVisitor = new CheckClassAdapter(classVisitor);
        }
        writeObject(classVisitor, objectDef);
        classVisitor.visitEnd();
        return classWriter;
    }

    /**
     * Write an object.
     *
     * @param classVisitor The class visitor
     * @param objectDef    The object definition
     */
    public void writeObject(ClassVisitor classVisitor, ObjectDef objectDef) {
        if (objectDef instanceof ClassDef classDef) {
            writeClass(classVisitor, classDef);
        } else if (objectDef instanceof RecordDef recordDef) {
            writeRecord(classVisitor, recordDef);
        } else if (objectDef instanceof InterfaceDef interfaceDef) {
            writeInterface(classVisitor, interfaceDef);
        } else if (objectDef instanceof EnumDef enumDef) {
            writeClass(classVisitor, EnumGenUtils.toClassDef(enumDef));
        } else {
            throw new UnsupportedOperationException("Unknown object definition: " + objectDef);
        }
    }

    private MethodDef createStaticInitializer(StatementDef statement) {
        return MethodDef.builder("<clinit>")
            .returns(TypeDef.VOID)
            .addModifiers(Modifier.STATIC)
            .addStatement(statement)
            .build();
    }

    /**
     * Write an enum.
     *
     * @param classVisitor The class visitor
     * @param objectDef    The object definition
     * @param fieldDef     The field definition
     */
    public void writeField(ClassVisitor classVisitor, ObjectDef objectDef, FieldDef fieldDef) {
        int modifiersFlag = getModifiersFlag(fieldDef.getModifiers());
        if (EnumGenUtils.isEnumField(objectDef, fieldDef)) {
            modifiersFlag |= ACC_ENUM;
        }
        FieldVisitor fieldVisitor = classVisitor.visitField(
            modifiersFlag,
            fieldDef.getName(),
            TypeUtils.getType(fieldDef.getType(), objectDef).getDescriptor(),
            SignatureWriterUtils.getFieldSignature(objectDef, fieldDef),
            null
        );
        for (AnnotationDef annotation : fieldDef.getAnnotations()) {
            AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(TypeUtils.getType(annotation.getType(), null).getDescriptor(), true);
            visitAnnotation(annotation, annotationVisitor);
        }
        fieldVisitor.visitEnd();
    }

    /**
     * Write an interface.
     *
     * @param classVisitor The class visitor
     * @param interfaceDef The interface definition
     */
    public void writeInterface(ClassVisitor classVisitor, InterfaceDef interfaceDef) {
        classVisitor.visit(V17,
            ACC_INTERFACE | ACC_ABSTRACT | getModifiersFlag(interfaceDef.getModifiers()),
            TypeUtils.getType(interfaceDef.asTypeDef()).getInternalName(),
            SignatureWriterUtils.getInterfaceSignature(interfaceDef),
            TypeUtils.OBJECT_TYPE.getInternalName(),
            interfaceDef.getSuperinterfaces().stream().map(i -> TypeUtils.getType(i, interfaceDef)).map(Type::getInternalName).toArray(String[]::new)
        );
        for (AnnotationDef annotation : interfaceDef.getAnnotations()) {
            AnnotationVisitor annotationVisitor = classVisitor.visitAnnotation(TypeUtils.getType(annotation.getType(), null).getDescriptor(), true);
            visitAnnotation(annotation, annotationVisitor);
        }
        for (MethodDef method : interfaceDef.getMethods()) {
            writeMethod(classVisitor, interfaceDef, method);
        }
        for (PropertyDef property : interfaceDef.getProperties()) {
            writeProperty(classVisitor, interfaceDef, property);
        }
    }

    /**
     * Write an interface.
     *
     * @param classVisitor The class visitor
     * @param recordDef    The record definition
     */
    public void writeRecord(ClassVisitor classVisitor, RecordDef recordDef) {
        classVisitor.visit(
            V17,
            ACC_RECORD | getModifiersFlag(recordDef.getModifiers()),
            TypeUtils.getType(recordDef.asTypeDef()).getInternalName(),
            SignatureWriterUtils.getRecordSignature(recordDef),
            Type.getType(Record.class).getInternalName(),
            recordDef.getSuperinterfaces().stream().map(i -> TypeUtils.getType(i, recordDef)).map(Type::getInternalName).toArray(String[]::new)
        );
    }

    /**
     * Write an interface.
     *
     * @param classVisitor The class visitor
     * @param classDef     The class definition
     */
    public void writeClass(ClassVisitor classVisitor, ClassDef classDef) {
        TypeDef superclass = Objects.requireNonNullElse(classDef.getSuperclass(), TypeDef.OBJECT);

        int modifiersFlag = getModifiersFlag(classDef.getModifiers());

        if (EnumGenUtils.isEnum(classDef)) {
            modifiersFlag |= ACC_ENUM;
        }

        classVisitor.visit(
            V17,
            modifiersFlag,
            TypeUtils.getType(classDef.asTypeDef()).getInternalName(),
            SignatureWriterUtils.getClassSignature(classDef),
            TypeUtils.getType(superclass, null).getInternalName(),
            classDef.getSuperinterfaces().stream().map(i -> TypeUtils.getType(i, classDef)).map(Type::getInternalName).toArray(String[]::new)
        );

        for (AnnotationDef annotation : classDef.getAnnotations()) {
            AnnotationVisitor annotationVisitor = classVisitor.visitAnnotation(
                TypeUtils.getType(annotation.getType(), null).getDescriptor(),
                true);
            visitAnnotation(annotation, annotationVisitor);
        }

        List<StatementDef> staticInitStatements = new ArrayList<>();
        for (FieldDef field : classDef.getFields()) {
            writeField(classVisitor, classDef, field);
            field.getInitializer().ifPresent(expressionDef -> {
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    staticInitStatements.add(classDef.asTypeDef().getStaticField(field).put(expressionDef));
                }
            });
        }

        StatementDef staticInitializer = classDef.getStaticInitializer();
        if (staticInitializer != null) {
            staticInitStatements.add(staticInitializer);
        }
        if (!staticInitStatements.isEmpty()) {
            writeMethod(classVisitor, null, createStaticInitializer(StatementDef.multi(staticInitStatements)));
        }

        if (classDef.getMethods().stream().noneMatch(MethodDef::isConstructor)) {
            // Add default constructor
            writeMethod(classVisitor, classDef, MethodDef.constructor()
                .build((aThis, methodParameters) -> aThis.superRef().invokeConstructor(methodParameters)));
        }

        for (PropertyDef property : classDef.getProperties()) {
            writeProperty(classVisitor, classDef, property);
        }
        for (MethodDef method : classDef.getMethods()) {
            writeMethod(classVisitor, classDef, method);
        }
    }

    private void visitAnnotation(AnnotationDef annotation, AnnotationVisitor annotationVisitor) {
        for (Map.Entry<String, Object> entry : annotation.getValues().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            visitAnnotation(annotationVisitor, key, value);
        }
        annotationVisitor.visitEnd();
    }

    private void visitAnnotation(AnnotationVisitor annotationVisitor, String name, Object value) {
        if (value instanceof VariableDef.StaticField staticField) {
            annotationVisitor.visitEnum(
                name,
                TypeUtils.getType(staticField.ownerType(), null).getDescriptor(),
                staticField.name()
            );
        } else if (value instanceof AnnotationDef nestedAnnotation) {
            visitAnnotation(
                nestedAnnotation,
                annotationVisitor.visitAnnotation(name, TypeUtils.getType(nestedAnnotation.getType(), null).getDescriptor())
            );
        } else if (value instanceof AnnotationDef[] annotations) {
            AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
            for (AnnotationDef annotationDef : annotations) {
                visitAnnotation(
                    annotationDef,
                    annotationVisitor.visitAnnotation(name, TypeUtils.getType(annotationDef.getType(), null).getDescriptor())
                );
            }
            arrayVisitor.visitEnd();
        } else if (value instanceof Collection<?> coll) {
            AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
            for (Object object : coll) {
                visitAnnotation(arrayVisitor, name, object);
            }
            arrayVisitor.visitEnd();
        } else if (value instanceof Object[] array) {
            AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
            for (Object object : array) {
                visitAnnotation(arrayVisitor, name, object);
            }
            arrayVisitor.visitEnd();
        } else {
            annotationVisitor.visit(name, value);
        }
    }

    private void writeProperty(ClassVisitor classWriter, ObjectDef objectDef, PropertyDef property) {
        FieldDef propertyField = FieldDef.builder(property.getName(), property.getType())
            .addModifiers(Modifier.PRIVATE)
            .addAnnotations(property.getAnnotations())
            .build();

        writeField(classWriter, objectDef, propertyField);

        String capitalizedPropertyName = NameUtils.capitalize(property.getName());

        boolean isAbstract = objectDef instanceof InterfaceDef;

        MethodDef.MethodDefBuilder getterBuilder = MethodDef.builder("get" + capitalizedPropertyName)
            .addModifiers(property.getModifiersArray());

        if (!isAbstract) {
            getterBuilder.addStatement((aThis, methodParameters) -> aThis.field(propertyField).returning());
        }

        writeMethod(classWriter, objectDef, getterBuilder.build());

        MethodDef.MethodDefBuilder setterBuilder = MethodDef.builder("set" + capitalizedPropertyName)
            .addParameter(ParameterDef.of(property.getName(), property.getType()))
            .addModifiers(property.getModifiersArray());

        if (!isAbstract) {
            setterBuilder.addStatement((aThis, methodParameters) -> aThis.field(propertyField).assign(methodParameters.get(0)));
        }

        writeMethod(classWriter, objectDef, setterBuilder.build());
    }

    /**
     * Write an interface.
     *
     * @param classVisitor The class visitor
     * @param objectDef    The object definition
     * @param methodDef    The method definition
     */
    public void writeMethod(ClassVisitor classVisitor, @Nullable ObjectDef objectDef, MethodDef methodDef) {
        String name = methodDef.getName();
        String methodDescriptor = getMethodDescriptor(objectDef, methodDef);
        int access = getModifiersFlag(methodDef.getModifiers());

        MethodVisitor methodVisitor = classVisitor.visitMethod(
            access,
            name,
            methodDescriptor,
            SignatureWriterUtils.getMethodSignature(objectDef, methodDef),
            null
        );
        GeneratorAdapter generatorAdapter = new GeneratorAdapter(methodVisitor, access, name, methodDescriptor);
        for (AnnotationDef annotation : methodDef.getAnnotations()) {
            methodVisitor.visitAnnotation(TypeUtils.getType(annotation.getType(), null).getDescriptor(), true);
        }

        if (methodDef.getParameters().stream().anyMatch(p -> !p.getAnnotations().isEmpty())) {
            methodVisitor.visitAnnotableParameterCount(methodDef.getParameters().size(), true);
        }

        int parameterIndex = 0;
        for (ParameterDef parameter : methodDef.getParameters()) {
            for (AnnotationDef annotation : parameter.getAnnotations()) {
                AnnotationVisitor annotationVisitor = methodVisitor.visitParameterAnnotation(parameterIndex, TypeUtils.getType(annotation.getType(), null).getDescriptor(), true);
                visitAnnotation(annotation, annotationVisitor);
            }
            parameterIndex++;
        }

        Context context = new Context(objectDef, methodDef);
        List<StatementDef> statements = methodDef.getStatements();
        if (methodDef.isConstructor()) {
            statements = adjustConstructorStatements(objectDef, statements);
        }
        if (!statements.isEmpty()) {
            methodVisitor.visitCode();
            for (StatementDef statement : statements) {
                pushStatement(generatorAdapter, context, statement, null);
            }
            StatementDef statementDef = statements.get(statements.size() - 1);
            if (!hasReturnStatement(statementDef)) {
                if (methodDef.getReturnType().equals(TypeDef.VOID)) {
                    generatorAdapter.returnValue();
                } else {
                    throw new IllegalStateException("The method: " + (objectDef == null ? "" : objectDef.getName()) + " " + methodDef.getName() + " doesn't return the result!");
                }
            }
        }
        if (visitMaxs && !statements.isEmpty()) {
            methodVisitor.visitMaxs(20, 20);
        }
        methodVisitor.visitEnd();
    }

    private List<StatementDef> adjustConstructorStatements(ObjectDef objectDef, List<StatementDef> statements) {
        if (!(objectDef instanceof ClassDef classDef)) {
            return statements;
        }
        List<StatementDef> fieldInitializers = classDef.getFields().stream().filter(fieldDef -> !fieldDef.getModifiers().contains(Modifier.STATIC))
            .flatMap(fieldDef -> fieldDef.getInitializer().<StatementDef>map(initializer -> new VariableDef.This().field(fieldDef).assign(initializer)).stream())
            .toList();
        Optional<StatementDef> constructorInvocation = statements.stream().filter(this::isConstructorInvocation).findFirst();
        if (constructorInvocation.isEmpty() || !fieldInitializers.isEmpty()) {
            // Add the constructor or reshuffle the statements to have the field initializers right after the constructor call
            List<StatementDef> newStatements = new ArrayList<>();
            // Constructor call
            newStatements.add(constructorInvocation.orElseGet(this::superConstructorInvocation));
            // Fields initializer
            newStatements.addAll(fieldInitializers);
            // Statements
            if (constructorInvocation.isPresent()) {
                // Remove constructor moved to the front
                List<StatementDef> statementsWithoutConstructor = new ArrayList<>(statements);
                statementsWithoutConstructor.remove(constructorInvocation.get());
                newStatements.addAll(statementsWithoutConstructor);
            } else {
                newStatements.addAll(statements);
            }
            statements = newStatements;
        }
        return statements;
    }

    private boolean hasReturnStatement(StatementDef statement) {
        List<StatementDef> statements = statement.flatten();
        StatementDef statementDef = statements.get(statements.size() - 1);
        if (statementDef instanceof StatementDef.IfElse ifElse) {
            return hasReturnStatement(ifElse.statement()) && hasReturnStatement(ifElse.elseStatement());
        }
        if (statementDef instanceof StatementDef.Try aTry) {
            return hasReturnStatement(aTry.statement());
        }
        if (statementDef instanceof StatementDef.Synchronized aSynchronized) {
            return hasReturnStatement(aSynchronized.statement());
        }
        if (statementDef instanceof StatementDef.Switch switchStatement) {
            if (switchStatement.defaultCase() == null) {
                return false;
            }
            return switchStatement.cases().values().stream().allMatch(this::hasReturnStatement);
        }
        return statementDef instanceof StatementDef.Return || statementDef instanceof StatementDef.Throw;
    }

    private StatementDef superConstructorInvocation() {
        return MethodDef.constructor().build((aThis, methodParameters) -> aThis.superRef().invokeConstructor())
            .getStatements()
            .get(0);
    }

    private boolean isConstructorInvocation(StatementDef statement) {
        return statement instanceof ExpressionDef.InvokeInstanceMethod call && call.method().isConstructor();
    }

    /**
     * Push the statement.
     *
     * @param generatorAdapter The adapter
     * @param context          The context
     * @param statementDef     The statement definition
     * @param finallyBlock     The runnable that should be invoked before any returning operation - return/throw
     */
    public void pushStatement(GeneratorAdapter generatorAdapter,
                              Context context,
                              StatementDef statementDef,
                              @Nullable Runnable finallyBlock) {
        if (statementDef instanceof StatementDef.Multi statements) {
            for (StatementDef statement : statements.statements()) {
                pushStatement(generatorAdapter, context, statement, finallyBlock);
            }
            return;
        }
        if (statementDef instanceof StatementDef.If ifStatement) {
            Label elseLabel = new Label();
            pushElseConditionalExpression(generatorAdapter, context, ifStatement.condition(), elseLabel);
            pushStatement(generatorAdapter, context, ifStatement.statement(), finallyBlock);
            generatorAdapter.visitLabel(elseLabel);
            return;
        }
        if (statementDef instanceof StatementDef.IfElse ifStatement) {
            Label elseLabel = new Label();
            pushElseConditionalExpression(generatorAdapter, context, ifStatement.condition(), elseLabel);
            Label end = new Label();
            pushStatement(generatorAdapter, context, ifStatement.statement(), finallyBlock);
            generatorAdapter.visitLabel(end);
            generatorAdapter.visitLabel(elseLabel);
            pushStatement(generatorAdapter, context, ifStatement.elseStatement(), finallyBlock);
            return;
        }
        if (statementDef instanceof StatementDef.Switch aSwitch) {
            boolean isStringSwitch = aSwitch.expression().type() instanceof ClassTypeDef classTypeDef && classTypeDef.getName().equals(String.class.getName());
            if (isStringSwitch) {
                pushStringSwitch(generatorAdapter, context, finallyBlock, aSwitch);
            } else {
                pushSwitch(generatorAdapter, context, finallyBlock, aSwitch);
            }
            return;
        }
        if (statementDef instanceof StatementDef.While aWhile) {
            Label whileLoop = new Label();
            Label end = new Label();
            generatorAdapter.visitLabel(whileLoop);
            pushExpression(generatorAdapter, context, aWhile.expression(), TypeDef.Primitive.BOOLEAN);
            generatorAdapter.push(true);
            generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, end);
            pushStatement(generatorAdapter, context, aWhile.statement(), finallyBlock);
            generatorAdapter.goTo(whileLoop);
            generatorAdapter.visitLabel(end);
            return;
        }
        if (statementDef instanceof StatementDef.Throw aThrow) {
            pushExpression(generatorAdapter, context, aThrow.expression(), aThrow.expression().type());
            generatorAdapter.throwException();
            return;
        }
        if (statementDef instanceof StatementDef.Return aReturn) {
            aReturn.validate(context.methodDef);
            if (aReturn.expression() != null) {
                pushExpression(generatorAdapter, context, aReturn.expression(), context.methodDef.getReturnType());
                pushFinallyStatement(generatorAdapter, context, finallyBlock, context.methodDef.getReturnType());
            } else {
                if (finallyBlock != null) {
                    finallyBlock.run();
                }
            }
            generatorAdapter.returnValue();
            return;
        }
        if (statementDef instanceof StatementDef.PutStaticField putStaticField) {
            VariableDef.StaticField field = putStaticField.field();
            pushExpression(generatorAdapter, context, putStaticField.expression(), field.type());
            generatorAdapter.putStatic(
                TypeUtils.getType(field.ownerType(), context.objectDef),
                field.name(),
                TypeUtils.getType(field.type(), context.objectDef)
            );
            return;
        }
        if (statementDef instanceof StatementDef.PutField putField) {
            VariableDef.Field field = putField.field();
            TypeDef owner = field.instance().type();
            pushExpression(generatorAdapter, context, field.instance(), owner);
            TypeDef fieldType = field.type();
            pushExpression(generatorAdapter, context, putField.expression(), fieldType);
            generatorAdapter.putField(
                TypeUtils.getType(owner, context.objectDef),
                field.name(),
                TypeUtils.getType(fieldType, context.objectDef)
            );
            return;
        }
        if (statementDef instanceof StatementDef.Assign assign) {
            VariableDef.Local local = assign.variable();
            Type localType = TypeUtils.getType(local.type(), context.objectDef);
            pushExpression(generatorAdapter, context, assign.expression(), local.type());
            Integer localIndex = context.locals.get(local.name());
            generatorAdapter.storeLocal(localIndex, localType);
            return;
        }
        if (statementDef instanceof StatementDef.DefineAndAssign assign) {
            VariableDef.Local local = assign.variable();
            Type localType = TypeUtils.getType(local.type(), context.objectDef);
            int localIndex = generatorAdapter.newLocal(localType);
            pushExpression(generatorAdapter, context, assign.expression(), local.type());
            generatorAdapter.storeLocal(localIndex, localType);
            context.locals.put(local.name(), localIndex);
            return;
        }
        if (statementDef instanceof StatementDef.Try aTry) {
            pushTryCatch(generatorAdapter, context, aTry, finallyBlock);
            return;
        }
        if (statementDef instanceof StatementDef.Synchronized aSynchronized) {
            pushSynchronized(generatorAdapter, context, aSynchronized, finallyBlock);
            return;
        }
        if (statementDef instanceof ExpressionDef expressionDef) {
            pushExpression(generatorAdapter, context, expressionDef, expressionDef.type(), true);
            return;
        }
        throw new UnsupportedOperationException("Unrecognized statement: " + statementDef);
    }

    private void pushSynchronized(GeneratorAdapter generatorAdapter, Context context, StatementDef.Synchronized aSynchronized, Runnable finallyBlock) {
        Label end = new Label();
        Label synchronizedStart = new Label();
        Label synchronizedEnd = new Label();
        Label synchronizedException = new Label();
        Label synchronizedExceptionEnd = new Label();
        generatorAdapter.visitTryCatchBlock(synchronizedStart, synchronizedEnd, synchronizedException, null);
        generatorAdapter.visitTryCatchBlock(synchronizedException, synchronizedExceptionEnd, synchronizedException, null);

        pushExpression(generatorAdapter, context, aSynchronized.monitor(), aSynchronized.monitor().type(), false);
        generatorAdapter.dup();
        Type monitorType = TypeUtils.getType(aSynchronized.monitor().type(), context.objectDef);
        int monitorLocal = storeLocal(generatorAdapter, monitorType);
        generatorAdapter.monitorEnter();

        generatorAdapter.visitLabel(synchronizedStart);

        pushStatement(generatorAdapter, context, aSynchronized.statement(), () -> {
            generatorAdapter.loadLocal(monitorLocal);
            generatorAdapter.monitorExit();
            if (finallyBlock != null) {
                finallyBlock.run();
            }
        });

        generatorAdapter.loadLocal(monitorLocal);
        generatorAdapter.monitorExit();
        generatorAdapter.visitLabel(synchronizedEnd);
        generatorAdapter.goTo(end);

        generatorAdapter.visitLabel(synchronizedException);
        // Insert the monitor exit before the exception throw
        Type throwableType = Type.getType(Throwable.class);
        int exceptionLocal = generatorAdapter.newLocal(throwableType);
        generatorAdapter.storeLocal(exceptionLocal);

        generatorAdapter.loadLocal(monitorLocal);
        generatorAdapter.monitorExit();

        generatorAdapter.visitLabel(synchronizedExceptionEnd);

        generatorAdapter.loadLocal(exceptionLocal, throwableType);
        generatorAdapter.throwException();

        generatorAdapter.visitLabel(end);
    }

    private void pushTryCatch(GeneratorAdapter generatorAdapter, Context context, StatementDef.Try aTry, Runnable finallyBlock) {
        Label end = new Label();
        Label tryStart = new Label();
        Label tryEnd = new Label();

        List<CatchBlock> exceptionHandlers = new ArrayList<>();

        for (StatementDef.Try.Catch aCatch : aTry.catches()) {
            Label exceptionHandler = new Label();

            exceptionHandlers.add(new CatchBlock(aCatch, exceptionHandler));

            generatorAdapter.visitTryCatchBlock(
                tryStart,
                tryEnd,
                exceptionHandler,
                TypeUtils.getType(aCatch.exception(), context.objectDef).getInternalName()
            );
        }

        Label finallyExceptionHandler = null;

        if (aTry.finallyStatement() != null) {
            finallyExceptionHandler = new Label();
            generatorAdapter.visitTryCatchBlock(
                tryStart,
                tryEnd,
                finallyExceptionHandler,
                null
            );
            for (CatchBlock catchBlock : exceptionHandlers) {
                catchBlock.to = new Label();
                generatorAdapter.visitTryCatchBlock(
                    catchBlock.from,
                    catchBlock.to,
                    finallyExceptionHandler,
                    null
                );
            }
        }

        generatorAdapter.visitLabel(tryStart);

        Runnable thisFinallyBlock = aTry.finallyStatement() == null ? null : () -> pushStatement(generatorAdapter, context, aTry.finallyStatement(), finallyBlock);
        pushStatement(generatorAdapter, context, aTry.statement(), thisFinallyBlock);

        generatorAdapter.visitLabel(tryEnd);
        generatorAdapter.goTo(end);

        for (CatchBlock catchBlock : exceptionHandlers) {
            StatementDef.Try.Catch aCatch = catchBlock.aCatch;
            generatorAdapter.visitLabel(catchBlock.from);

            Type exceptionType = TypeUtils.getType(aCatch.exception(), context.objectDef);
            int local = generatorAdapter.newLocal(exceptionType);
            generatorAdapter.storeLocal(local);
            context.locals().put("@exception", local);

            pushStatement(generatorAdapter, context, aCatch.statement(), thisFinallyBlock);

            context.locals().remove("@exception");

            if (catchBlock.to != null) {
                generatorAdapter.visitLabel(catchBlock.to);
            }

            if (aTry.finallyStatement() != null) {
                pushStatement(generatorAdapter, context, aTry.finallyStatement(), finallyBlock);
            }

            generatorAdapter.goTo(end);
        }

        if (finallyExceptionHandler != null) {
            generatorAdapter.visitLabel(finallyExceptionHandler);

            Type exceptionType = TypeUtils.getType(TypeDef.of(Throwable.class), context.objectDef);
            int local = generatorAdapter.newLocal(exceptionType);
            generatorAdapter.storeLocal(local);

            pushStatement(generatorAdapter, context, aTry.finallyStatement(), finallyBlock);

            generatorAdapter.loadLocal(local);
            generatorAdapter.throwException();

            generatorAdapter.goTo(end);
        }

        generatorAdapter.visitLabel(end);
    }

    private void pushSwitch(GeneratorAdapter generatorAdapter, Context context, Runnable finallyBlock, StatementDef.Switch aSwitch) {
        pushSwitchExpression(generatorAdapter, context, aSwitch.expression());
        Map<Integer, StatementDef> map = aSwitch.cases().entrySet().stream().map(e -> Map.entry(toSwitchKey(e.getKey()), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tableSwitch(generatorAdapter, context, map, aSwitch.defaultCase(), finallyBlock);
    }

    private void pushStringSwitch(GeneratorAdapter generatorAdapter, Context context, Runnable finallyBlock, StatementDef.Switch aSwitch) {
        ExpressionDef expression = aSwitch.expression();
        TypeDef switchExpressionType = expression.type();

        pushExpression(generatorAdapter, context, expression, switchExpressionType);

        Type stringType = Type.getType(String.class);
        int switchValueLocal = generatorAdapter.newLocal(stringType);
        generatorAdapter.storeLocal(switchValueLocal, stringType);
        generatorAdapter.loadLocal(switchValueLocal, stringType);
        generatorAdapter.invokeVirtual(
            stringType,
            Method.getMethod(ReflectionUtils.getRequiredMethod(String.class, "hashCode"))
        );

        Map<Integer, Map.Entry<ExpressionDef.Constant, StatementDef>> map = aSwitch.cases().entrySet().stream()
            .map(e -> Map.entry(toSwitchKey(e.getKey()), Map.entry(e.getKey(), e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int[] keys = map.keySet().stream().mapToInt(x -> x).sorted().toArray();
        Label defaultEnd = new Label();
        Label finalEnd = new Label();
        generatorAdapter.tableSwitch(keys, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                Map.Entry<ExpressionDef.Constant, StatementDef> e = map.get(key);
                ExpressionDef.Constant constant = e.getKey();
                if (!(constant.value() instanceof String stringValue)) {
                    throw new IllegalStateException("Expected a string value got: " + constant);
                }

                generatorAdapter.loadLocal(switchValueLocal, stringType);
                generatorAdapter.push(stringValue);
                generatorAdapter.invokeVirtual(stringType, Method.getMethod(ReflectionUtils.getRequiredMethod(String.class, "equals", Object.class)));
                generatorAdapter.push(true);
                generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, defaultEnd);
                pushStatement(generatorAdapter, context, e.getValue(), finallyBlock);
                generatorAdapter.goTo(finalEnd);
            }

            @Override
            public void generateDefault() {
                generatorAdapter.goTo(defaultEnd);
            }
        });

        generatorAdapter.visitLabel(defaultEnd);
        if (aSwitch.defaultCase() != null) {
            pushStatement(generatorAdapter, context, aSwitch.defaultCase(), finallyBlock);
        }
        generatorAdapter.visitLabel(finalEnd);
    }

    private int storeLocal(GeneratorAdapter generatorAdapter, Type type) {
        int ff = generatorAdapter.newLocal(type);
        generatorAdapter.storeLocal(ff);
        return ff;
    }

    private void pushFinallyStatement(GeneratorAdapter generatorAdapter, Context context, Runnable finallyBlock, TypeDef expTypeDef) {
        if (finallyBlock != null) {
            if (expTypeDef.equals(TypeDef.VOID)) {
                finallyBlock.run();
            } else {
                Type expType = TypeUtils.getType(expTypeDef, context.objectDef);
                int returnLocal = generatorAdapter.newLocal(expType);
                generatorAdapter.storeLocal(returnLocal);
                finallyBlock.run();
                generatorAdapter.loadLocal(returnLocal);
            }
        }
    }

    public void pushExpression(GeneratorAdapter generatorAdapter,
                               Context context,
                               ExpressionDef expressionDef,
                               TypeDef expectedType) {
        pushExpression(generatorAdapter, context, expressionDef, expectedType, false);
    }

    private void pushExpression(GeneratorAdapter generatorAdapter,
                                Context context,
                                ExpressionDef expressionDef,
                                TypeDef expectedType,
                                boolean statement) {
        if (expectedType.isPrimitive() &&
            expressionDef instanceof ExpressionDef.Constant constant
            && !constant.type().isPrimitive()
            && constant.value() != null
            && ReflectionUtils.getPrimitiveType(constant.value().getClass()).isPrimitive()) {
            expressionDef = ExpressionDef.primitiveConstant(constant.value());
        }
        pushExpressionNoCast(generatorAdapter, context, expressionDef, statement);
        TypeDef type = expressionDef.type();
        cast(generatorAdapter, context, type, expectedType);
    }

    private void cast(GeneratorAdapter generatorAdapter, Context context, TypeDef from, TypeDef to) {
        from = ObjectDef.getContextualType(context.objectDef, from);
        to = ObjectDef.getContextualType(context.objectDef, to);
        if ((from instanceof TypeDef.Primitive fromP && to instanceof TypeDef.Primitive toP) && !from.equals(to)) {
            generatorAdapter.cast(TypeUtils.getType(fromP), TypeUtils.getType(toP));
            return;
        }
        if ((from.isPrimitive() || to.isPrimitive()) && !from.equals(to)) {
            if (from instanceof TypeDef.Primitive primitive && !to.isPrimitive()) {
                box(generatorAdapter, context, from);
                checkCast(generatorAdapter, context, primitive.wrapperType(), to);
            }
            if (!from.isPrimitive() && to.isPrimitive()) {
                unbox(generatorAdapter, context, to);
            }
        } else if (!from.makeNullable().equals(to.makeNullable())) {
            if (from instanceof ClassTypeDef.ClassElementType fromElement) {
                ClassElement fromClassElement = fromElement.classElement();
                if (to instanceof ClassTypeDef.ClassElementType toElement) {
                    if (!fromClassElement.isAssignable(toElement.classElement())) {
                        checkCast(generatorAdapter, context, from, to);
                    }
                } else if (to instanceof ClassTypeDef.JavaClass toClass) {
                    if (!fromClassElement.isAssignable(toClass.type())) {
                        checkCast(generatorAdapter, context, from, to);
                    }
                } else if (to instanceof ClassTypeDef.ClassName toClassName) {
                    if (!fromClassElement.isAssignable(toClassName.className())) {
                        checkCast(generatorAdapter, context, from, to);
                    }
                } else {
                    checkCast(generatorAdapter, context, from, to);
                }
            } else if (from instanceof ClassTypeDef.JavaClass fromClass && to instanceof ClassTypeDef.JavaClass toClass) {
                if (!toClass.type().isAssignableFrom(fromClass.type())) {
                    checkCast(generatorAdapter, context, from, to);
                }
            } else {
                checkCast(generatorAdapter, context, from, to);
            }
        }
    }

    private void unbox(GeneratorAdapter generatorAdapter, Context context, TypeDef to) {
        generatorAdapter.unbox(TypeUtils.getType(to, context.objectDef));
    }

    private void box(GeneratorAdapter generatorAdapter, Context context, TypeDef from) {
        generatorAdapter.valueOf(TypeUtils.getType(from, context.objectDef));
    }

    private void pushExpressionNoCast(GeneratorAdapter generatorAdapter,
                                      Context context,
                                      ExpressionDef expressionDef,
                                      boolean statement) {
        if (expressionDef instanceof ExpressionDef.ArrayElement arrayElement) {
            pushExpression(generatorAdapter, context, arrayElement.expression(), arrayElement.expression().type());
            generatorAdapter.push(arrayElement.index());
            generatorAdapter.arrayLoad(TypeUtils.getType(arrayElement.type(), context.objectDef));
            return;
        }
        if (expressionDef instanceof ExpressionDef.InstanceOf instanceOf) {
            pushExpression(generatorAdapter, context, instanceOf.expression(), instanceOf.expression().type());
            generatorAdapter.instanceOf(TypeUtils.getType(instanceOf.instanceType(), context.objectDef));
            return;
        }
        if (expressionDef instanceof ExpressionDef.ConditionExpressionDef) {
            Label elseLabel = new Label();
            pushElseConditionalExpression(generatorAdapter, context, expressionDef, elseLabel);
            generatorAdapter.push(true);
            Label end = new Label();
            generatorAdapter.goTo(end);
            generatorAdapter.visitLabel(elseLabel);
            generatorAdapter.push(false);
            generatorAdapter.visitLabel(end);
            return;
        }
        if (expressionDef instanceof ExpressionDef.MathOp math) {
            pushExpression(generatorAdapter, context, math.left(), math.left().type());
            pushExpression(generatorAdapter, context, math.right(), math.right().type());
            generatorAdapter.math(getMathOp(math.operator()), TypeUtils.getType(math.left().type(), context.objectDef));
            return;
        }
        if (expressionDef instanceof ExpressionDef.InvokeInstanceMethod invokeInstanceMethod) {
            pushInvokeInstance(generatorAdapter, context, invokeInstanceMethod, statement);
            return;
        }
        if (expressionDef instanceof ExpressionDef.NewInstance newInstance) {
            Type type = TypeUtils.getType(newInstance.type(), context.objectDef);
            generatorAdapter.newInstance(type);
            generatorAdapter.dup();
            Iterator<TypeDef> iterator = newInstance.parameterTypes().iterator();
            for (ExpressionDef expression : newInstance.values()) {
                pushExpression(generatorAdapter, context, expression, iterator.next());
            }
            generatorAdapter.invokeConstructor(
                type,
                new Method("<init>", getConstructorDescriptor(context.objectDef, newInstance.parameterTypes()))
            );
            return;
        }
        if (expressionDef instanceof ExpressionDef.NewArrayOfSize newArray) {
            generatorAdapter.push(newArray.size());
            generatorAdapter.newArray(TypeUtils.getType(newArray.type().componentType(), context.objectDef));
            return;
        }
        if (expressionDef instanceof ExpressionDef.NewArrayInitialized newArray) {
            List<? extends ExpressionDef> expressions = newArray.expressions();
            generatorAdapter.push(expressions.size());
            TypeDef.Array arrayType = newArray.type();
            TypeDef componentType = arrayType.componentType();
            if (arrayType.dimensions() > 1) {
                componentType = componentType.array(arrayType.dimensions() - 1);
            }

            Type type = TypeUtils.getType(componentType, context.objectDef);
            generatorAdapter.newArray(type);

            if (!expressions.isEmpty()) {
                int index = 0;
                for (ExpressionDef expression : expressions) {
                    generatorAdapter.dup();
                    generatorAdapter.push(index++);
                    pushExpression(generatorAdapter, context, expression, componentType);
                    generatorAdapter.arrayStore(type);
                }
            }
            return;
        }
        if (expressionDef instanceof ExpressionDef.Cast castExpressionDef) {
            ExpressionDef exp = castExpressionDef.expressionDef();
            TypeDef from = exp.type();
            pushExpression(generatorAdapter, context, exp, from);
            TypeDef to = castExpressionDef.type();
            cast(generatorAdapter, context, from, to);
            return;
        }
        if (expressionDef instanceof ExpressionDef.Constant constant) {
            pushConstant(generatorAdapter, constant, context.objectDef);
            return;
        }
        if (expressionDef instanceof ExpressionDef.InvokeStaticMethod invokeStaticMethod) {
            pushInvokeStaticMethod(generatorAdapter, context, invokeStaticMethod, statement);
            return;
        }
        if (expressionDef instanceof ExpressionDef.GetPropertyValue getPropertyValue) {
            ExpressionDef propertyValue = JavaIdioms.getPropertyValue(getPropertyValue);
            pushExpression(generatorAdapter, context, propertyValue, getPropertyValue.type());
            return;
        }
        if (expressionDef instanceof ExpressionDef.IfElse conditionIfElse) {
            Label elseLabel = new Label();
            pushElseConditionalExpression(generatorAdapter, context, conditionIfElse.condition(), elseLabel);
            Label end = new Label();
            pushExpression(generatorAdapter, context, conditionIfElse.expression(), conditionIfElse.type());
            generatorAdapter.goTo(end);
            generatorAdapter.visitLabel(elseLabel);
            pushExpression(generatorAdapter, context, conditionIfElse.elseExpression(), conditionIfElse.type());
            generatorAdapter.visitLabel(end);
            return;
        }
        if (expressionDef instanceof ExpressionDef.Switch aSwitch) {
            ExpressionDef expression = aSwitch.expression();
            boolean isStringSwitch = expression.type() instanceof ClassTypeDef classTypeDef && classTypeDef.getName().equals(String.class.getName());
            if (isStringSwitch) {
                pushStringSwitch(generatorAdapter, context, aSwitch);
            } else {
                pushSwitch(generatorAdapter, context, aSwitch);
            }
            return;
        }
        if (expressionDef instanceof ExpressionDef.SwitchYieldCase switchYieldCase) {
            pushStatement(generatorAdapter, context, switchYieldCase.statement(), null);
            return;
        }
        if (expressionDef instanceof VariableDef variableDef) {
            pushVariable(generatorAdapter, context, variableDef);
            return;
        }
        if (expressionDef instanceof ExpressionDef.InvokeGetClassMethod invokeGetClassMethod) {
            pushExpression(generatorAdapter, context, JavaIdioms.getClass(invokeGetClassMethod), TypeDef.CLASS);
            return;
        }
        if (expressionDef instanceof ExpressionDef.InvokeHashCodeMethod invokeHashCodeMethod) {
            pushExpression(generatorAdapter, context, JavaIdioms.hashCode(invokeHashCodeMethod), TypeDef.Primitive.INT);
            return;
        }
        throw new UnsupportedOperationException("Unrecognized expression: " + expressionDef);
    }

    private void pushInvokeStaticMethod(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.InvokeStaticMethod invokeStaticMethod, boolean statement) {
        Iterator<ParameterDef> iterator = invokeStaticMethod.method().getParameters().iterator();
        for (ExpressionDef value : invokeStaticMethod.values()) {
            pushExpression(generatorAdapter, context, value, iterator.next().getType());
        }
        boolean isDeclaringTypeInterface = invokeStaticMethod.classDef().isInterface();
        MethodDef methodDef = invokeStaticMethod.method();
        Method method = asMethod(context, methodDef);
        Type type = TypeUtils.getType(invokeStaticMethod.classDef(), context.objectDef);

        String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
        generatorAdapter.visitMethodInsn(
            INVOKESTATIC,
            owner,
            method.getName(),
            method.getDescriptor(),
            isDeclaringTypeInterface);
        if (!methodDef.getReturnType().equals(TypeDef.VOID) && statement) {
            popValue(generatorAdapter, methodDef.getReturnType());
        }
    }

    private void pushInvokeInstance(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.InvokeInstanceMethod invokeInstanceMethod, boolean statement) {
        ExpressionDef instance = invokeInstanceMethod.instance();
        TypeDef instanceType = instance.type();
        pushExpression(generatorAdapter, context, instance, instanceType);
        Iterator<ParameterDef> iterator = invokeInstanceMethod.method().getParameters().iterator();
        for (ExpressionDef parameter : invokeInstanceMethod.values()) {
            pushExpression(generatorAdapter, context, parameter, iterator.next().getType());
        }
        Type methodOwnerType = TypeUtils.getType(instanceType, context.objectDef);
        Method method = asMethod(context, invokeInstanceMethod.method());
        if (invokeInstanceMethod.method().isConstructor()) {
            generatorAdapter.invokeConstructor(methodOwnerType, method);
        } else if (instanceType instanceof ClassTypeDef classTypeDef) {
            if (instance instanceof VariableDef.Super aSuper) {
                ClassTypeDef superClass;
                if (aSuper.type() == TypeDef.SUPER) {
                    if (context.objectDef instanceof EnumDef) {
                        superClass = ClassTypeDef.of(Enum.class);
                    } else if (context.objectDef instanceof ClassDef classDef) {
                        superClass = Objects.requireNonNullElse(classDef.getSuperclass(), TypeDef.OBJECT);
                    } else {
                        superClass = TypeDef.OBJECT;
                    }
                } else {
                    superClass = aSuper.type();
                }
                methodOwnerType = TypeUtils.getType(superClass, context.objectDef);
                generatorAdapter.visitMethodInsn(
                    INVOKESPECIAL,
                    methodOwnerType.getSort() == Type.ARRAY ? methodOwnerType.getDescriptor() : methodOwnerType.getInternalName(),
                    method.getName(),
                    method.getDescriptor(),
                    superClass.isInterface() && invokeInstanceMethod.isDefault()
                );
            } else if (classTypeDef.isInterface()) {
                generatorAdapter.invokeInterface(methodOwnerType, method);
            } else {
                generatorAdapter.invokeVirtual(methodOwnerType, method);
            }
        } else if (instanceType instanceof TypeDef.Array) {
            generatorAdapter.invokeVirtual(methodOwnerType, method);
        }
        if (!invokeInstanceMethod.method().getReturnType().equals(TypeDef.VOID) && statement) {
            popValue(generatorAdapter, invokeInstanceMethod.method().getReturnType());
        }
    }

    private void pushSwitch(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.Switch aSwitch) {
        ExpressionDef expression = aSwitch.expression();
        pushSwitchExpression(generatorAdapter, context, expression);
        Map<Integer, ExpressionDef> map = aSwitch.cases().entrySet().stream().map(e -> Map.entry(toSwitchKey(e.getKey()), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int[] keys = map.keySet().stream().mapToInt(x -> x).sorted().toArray();
        generatorAdapter.tableSwitch(keys, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                ExpressionDef exp = map.get(key);
                pushExpression(generatorAdapter, context, exp, aSwitch.type());
                generatorAdapter.goTo(end);
            }

            @Override
            public void generateDefault() {
                pushExpression(generatorAdapter, context, aSwitch.defaultCase(), aSwitch.type());
            }
        });
    }

    private void pushStringSwitch(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.Switch aSwitch) {
        ExpressionDef expression = aSwitch.expression();
        TypeDef switchExpressionType = expression.type();
        pushExpression(generatorAdapter, context, expression, switchExpressionType);

        Type stringType = Type.getType(String.class);
        int switchValueLocal = generatorAdapter.newLocal(stringType);
        generatorAdapter.storeLocal(switchValueLocal, stringType);
        generatorAdapter.loadLocal(switchValueLocal, stringType);
        generatorAdapter.invokeVirtual(
            stringType,
            Method.getMethod(ReflectionUtils.getRequiredMethod(String.class, "hashCode"))
        );

        Map<Integer, Map.Entry<ExpressionDef.Constant, ? extends ExpressionDef>> map = aSwitch.cases().entrySet().stream()
            .map(e -> Map.entry(toSwitchKey(e.getKey()), Map.entry(e.getKey(), e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int[] keys = map.keySet().stream().mapToInt(x -> x).sorted().toArray();
        Label defaultEnd = new Label();
        Label finalEnd = new Label();
        generatorAdapter.tableSwitch(keys, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                Map.Entry<ExpressionDef.Constant, ? extends ExpressionDef> e = map.get(key);
                if (!(e.getKey().value() instanceof String stringValue)) {
                    throw new IllegalStateException("Expected a switch string value got " + e.getKey());
                }
                generatorAdapter.loadLocal(switchValueLocal, stringType);
                generatorAdapter.push(stringValue);
                generatorAdapter.invokeVirtual(stringType, Method.getMethod(ReflectionUtils.getRequiredMethod(String.class, "equals", Object.class)));
                generatorAdapter.push(true);
                generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, defaultEnd);
                pushExpression(generatorAdapter, context, e.getValue(), aSwitch.type());
                generatorAdapter.goTo(finalEnd);
            }

            @Override
            public void generateDefault() {
                generatorAdapter.goTo(defaultEnd);
            }
        });

        generatorAdapter.visitLabel(defaultEnd);
        pushExpression(generatorAdapter, context, aSwitch.defaultCase(), aSwitch.type());
        generatorAdapter.visitLabel(finalEnd);
    }

    private int getMathOp(String op) {
        return switch (op.trim()) {
            case "+" -> GeneratorAdapter.ADD;
            case "*" -> GeneratorAdapter.MUL;
            case "|" -> GeneratorAdapter.OR;
            default -> throw new UnsupportedOperationException("Unrecognized math operator: " + op);
        };
    }

    private int getConditionOp(String op) {
        return switch (op.trim()) {
            case "==" -> GeneratorAdapter.EQ;
            case "!=" -> GeneratorAdapter.NE;
            default -> throw new UnsupportedOperationException("Unrecognized condition operator: " + op);
        };
    }

    private int getInvertConditionOp(String op) {
        int conditionOp = getConditionOp(op);
        return switch (conditionOp) {
            case GeneratorAdapter.EQ -> GeneratorAdapter.NE;
            case GeneratorAdapter.NE -> GeneratorAdapter.EQ;
            default -> throw new UnsupportedOperationException("Unrecognized condition operator: " + conditionOp);
        };
    }

    private void popValue(GeneratorAdapter generatorAdapter, TypeDef typeDef) {
        if (typeDef.equals(TypeDef.Primitive.LONG) || typeDef.equals(TypeDef.Primitive.DOUBLE)) {
            generatorAdapter.pop2();
        } else {
            generatorAdapter.pop();
        }
    }

    private Method asMethod(Context context, MethodDef methodDef) {
        return new Method(methodDef.getName(), getMethodDescriptor(context.objectDef, methodDef));
    }

    private void checkCast(GeneratorAdapter generatorAdapter, Context context, TypeDef from, TypeDef to) {
        TypeDef toType = ObjectDef.getContextualType(context.objectDef, to);
        if (!toType.makeNullable().equals(from.makeNullable())) {
            generatorAdapter.checkCast(TypeUtils.getType(toType, context.objectDef));
        }
    }

    private void pushSwitchExpression(GeneratorAdapter generatorAdapter,
                                      Context context,
                                      ExpressionDef expression) {
        TypeDef switchExpressionType = expression.type();
        pushExpression(generatorAdapter, context, expression, switchExpressionType);
        if (!switchExpressionType.equals(TypeDef.Primitive.INT)) {
            throw new UnsupportedOperationException("Not allowed switch expression type: " + switchExpressionType);
        }
    }

    private int toSwitchKey(ExpressionDef.Constant constant) {
        if (constant.value() instanceof String s) {
            return s.hashCode();
        }
        if (constant.value() instanceof Integer i) {
            return i;
        }
        throw new UnsupportedOperationException("Unrecognized constant for a switch key: " + constant);
    }

    private void pushElseConditionalExpression(GeneratorAdapter generatorAdapter,
                                               Context context,
                                               ExpressionDef expressionDef,
                                               Label elseLabel) {
        if (expressionDef instanceof ExpressionDef.ConditionExpressionDef conditionExpressionDef) {
            if (expressionDef instanceof ExpressionDef.InstanceOf instanceOf) {
                pushExpression(generatorAdapter, context, instanceOf.expression(), instanceOf.expression().type());
                generatorAdapter.instanceOf(TypeUtils.getType(instanceOf.instanceType(), context.objectDef));
                generatorAdapter.push(true);
                generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.And andExpressionDef) {
                pushElseConditionalExpression(generatorAdapter, context, andExpressionDef.left(), elseLabel);
                pushElseConditionalExpression(generatorAdapter, context, andExpressionDef.right(), elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.Or orExpressionDef) {
                Label ifLabel = new Label();
                pushIfConditionalExpression(generatorAdapter, context, orExpressionDef.left(), ifLabel);
                pushIfConditionalExpression(generatorAdapter, context, orExpressionDef.right(), ifLabel);
                generatorAdapter.goTo(elseLabel);
                generatorAdapter.visitLabel(ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.Condition condition) {
                pushExpression(generatorAdapter, context, condition.left(), condition.left().type());
                pushExpression(generatorAdapter, context, condition.right(), condition.right().type());
                Type conditionType = TypeUtils.getType(condition.left().type(), context.objectDef);
                generatorAdapter.ifCmp(conditionType, getInvertConditionOp(condition.operator()), elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.IsNull isNull) {
                pushExpression(generatorAdapter, context, isNull.expression(), isNull.expression().type());
                generatorAdapter.ifNonNull(elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.IsNotNull isNotNull) {
                pushExpression(generatorAdapter, context, isNotNull.expression(), isNotNull.expression().type());
                generatorAdapter.ifNull(elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.EqualsReferentially equalsReferentially) {
                pushEqualsReferentially(generatorAdapter, context, equalsReferentially, elseLabel, GeneratorAdapter.NE);
                return;
            }
            if (expressionDef instanceof ExpressionDef.EqualsStructurally equalsStructurally) {
                pushEqualsStructurally(generatorAdapter, context, equalsStructurally, elseLabel, GeneratorAdapter.NE);
                return;
            }
            throw new UnsupportedOperationException("Unrecognized conditional expression: " + conditionExpressionDef);
        }
        if (!expressionDef.type().equals(TypeDef.Primitive.BOOLEAN) && !expressionDef.type().equals(TypeDef.Primitive.BOOLEAN.wrapperType())) {
            throw new IllegalStateException("Conditional expression should produce a boolean: " + expressionDef);
        }
        pushExpression(generatorAdapter, context, expressionDef, TypeDef.Primitive.BOOLEAN);
        generatorAdapter.push(true);
        generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);
    }

    private void pushIfConditionalExpression(GeneratorAdapter generatorAdapter,
                                             Context context,
                                             ExpressionDef expressionDef,
                                             Label ifLabel) {
        if (expressionDef instanceof ExpressionDef.ConditionExpressionDef conditionExpressionDef) {
            if (expressionDef instanceof ExpressionDef.InstanceOf instanceOf) {
                pushExpression(generatorAdapter, context, instanceOf.expression(), instanceOf.expression().type());
                generatorAdapter.instanceOf(TypeUtils.getType(instanceOf.instanceType(), context.objectDef));
                generatorAdapter.push(true);
                generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.And andExpressionDef) {
                Label elseLabel = new Label();
                pushElseConditionalExpression(generatorAdapter, context, andExpressionDef.left(), elseLabel);
                pushElseConditionalExpression(generatorAdapter, context, andExpressionDef.right(), elseLabel);
                generatorAdapter.goTo(ifLabel);
                generatorAdapter.visitLabel(elseLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.Or orExpressionDef) {
                pushIfConditionalExpression(generatorAdapter, context, orExpressionDef.left(), ifLabel);
                pushIfConditionalExpression(generatorAdapter, context, orExpressionDef.right(), ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.Condition condition) {
                pushExpression(generatorAdapter, context, condition.left(), condition.left().type());
                pushExpression(generatorAdapter, context, condition.right(), condition.right().type());
                Type conditionType = TypeUtils.getType(condition.left().type(), context.objectDef);
                generatorAdapter.ifCmp(conditionType, getConditionOp(condition.operator()), ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.IsNull isNull) {
                pushExpression(generatorAdapter, context, isNull.expression(), isNull.expression().type());
                generatorAdapter.ifNull(ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.IsNotNull isNotNull) {
                pushExpression(generatorAdapter, context, isNotNull.expression(), isNotNull.expression().type());
                generatorAdapter.ifNonNull(ifLabel);
                return;
            }
            if (conditionExpressionDef instanceof ExpressionDef.EqualsReferentially equalsReferentially) {
                pushEqualsReferentially(generatorAdapter, context, equalsReferentially, ifLabel, GeneratorAdapter.EQ);
                return;
            }
            if (expressionDef instanceof ExpressionDef.EqualsStructurally equalsStructurally) {
                pushEqualsStructurally(generatorAdapter, context, equalsStructurally, ifLabel, GeneratorAdapter.EQ);
                return;
            }
            throw new UnsupportedOperationException("Unrecognized conditional expression: " + conditionExpressionDef);
        }
        if (!expressionDef.type().equals(TypeDef.Primitive.BOOLEAN) && !expressionDef.type().equals(TypeDef.Primitive.BOOLEAN.wrapperType())) {
            throw new IllegalStateException("Conditional expression should produce a boolean: " + expressionDef);
        }
        pushExpression(generatorAdapter, context, expressionDef, TypeDef.Primitive.BOOLEAN);
        generatorAdapter.push(true);
        generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, ifLabel);
    }

    private void pushEqualsStructurally(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.EqualsStructurally equalsStructurally, Label ifLabel, int op) {
        TypeDef leftType = equalsStructurally.instance().type();
        TypeDef rightType = equalsStructurally.other().type();
        if (leftType.isPrimitive()) {
            pushEqualsReferentially(generatorAdapter, context, equalsStructurally.instance(), equalsStructurally.other().cast(leftType), ifLabel, op);
            return;
        }
        if (rightType.isPrimitive()) {
            pushEqualsReferentially(generatorAdapter, context, equalsStructurally.instance().cast(rightType), equalsStructurally.other(), ifLabel, op);
            return;
        }
        pushEqualsStructurally(generatorAdapter, context, equalsStructurally);
        generatorAdapter.push(true);
        generatorAdapter.ifCmp(Type.BOOLEAN_TYPE, op, ifLabel);
    }

    private void pushEqualsReferentially(GeneratorAdapter generatorAdapter, Context context,
                                         ExpressionDef.EqualsReferentially equalsReferentially,
                                         Label label, int op) {
        ExpressionDef left = equalsReferentially.instance();
        ExpressionDef right = equalsReferentially.other();
        pushEqualsReferentially(generatorAdapter, context, left, right, label, op);
    }

    private void pushEqualsReferentially(GeneratorAdapter generatorAdapter,
                                         Context context,
                                         ExpressionDef left,
                                         ExpressionDef right,
                                         Label label,
                                         int op) {
        TypeDef leftType = left.type();
        pushExpression(generatorAdapter, context, left, leftType);
        TypeDef rightType = right.type();
        pushExpression(generatorAdapter, context, right, rightType);
        if (leftType instanceof TypeDef.Primitive p1 && rightType instanceof TypeDef.Primitive) {
            generatorAdapter.ifCmp(TypeUtils.getType(p1), op, label);
        } else {
            generatorAdapter.ifCmp(TypeUtils.OBJECT_TYPE, op, label);
        }
    }

    private void pushEqualsStructurally(GeneratorAdapter generatorAdapter, Context context, ExpressionDef.EqualsStructurally equalsStructurally) {
        pushExpression(generatorAdapter, context, JavaIdioms.equalsStructurally(equalsStructurally), TypeDef.Primitive.BOOLEAN);
    }

    private void pushVariable(GeneratorAdapter generatorAdapter,
                              Context context,
                              VariableDef variableDef) {
        if (variableDef instanceof VariableDef.ExceptionVar exceptionVar) {
            int index = context.locals.get("@exception");
            generatorAdapter.loadLocal(index, TypeUtils.getType(exceptionVar.type(), context.objectDef));
            return;
        }
        if (variableDef instanceof VariableDef.Local localVariableDef) {
            int index = context.locals.get(localVariableDef.name());
            generatorAdapter.loadLocal(index, TypeUtils.getType(localVariableDef.type(), context.objectDef));
            return;
        }
        if (variableDef instanceof VariableDef.MethodParameter parameterVariableDef) {
            if (context.methodDef == null) {
                throw new IllegalStateException("Accessing method parameters is not available");
            }
            ParameterDef parameterDef = context.methodDef.getParameters().stream().filter(p -> p.getName().equals(parameterVariableDef.name())).findFirst().orElseThrow();
            int parameterIndex = context.methodDef.getParameters().indexOf(parameterDef);
            generatorAdapter.loadArg(parameterIndex);
            return;
        }
        if (variableDef instanceof VariableDef.StaticField field) {
//            if (context.objectDef instanceof ClassDef classDef) {
//                if (!classDef.hasField(field.name()) && classDef.getProperties().stream().noneMatch(prop -> prop.getName().equals(field.name()))) {
//                    throw new IllegalStateException("Field '" + field.name() + "' is not available in [" + classDef + "]:" + classDef.getFields());
//                }
//            } else {
//                throw new IllegalStateException("Field access not supported on the object definition: " + context.objectDef);
//            }
            TypeDef owner = field.ownerType();
            TypeDef fieldType = field.type();

            generatorAdapter.getStatic(TypeUtils.getType(owner, context.objectDef), field.name(), TypeUtils.getType(fieldType, context.objectDef));
            return;
        }
        if (variableDef instanceof VariableDef.Field field) {
            TypeDef owner = field.instance().type();
//            if (context.objectDef == null) {
//                throw new IllegalStateException("Accessing 'this' is not available");
//            }
//            if (context.objectDef instanceof ClassDef classDef) {
//                if (!classDef.hasField(field.name()) && classDef.getProperties().stream().noneMatch(prop -> prop.getName().equals(field.name()))) {
//                    throw new IllegalStateException("Field '" + field.name() + "' is not available in [" + classDef + "]:" + classDef.getFields());
//                }
//            } else {
//                throw new IllegalStateException("Field access not supported on the object definition: " + context.objectDef);
//            }

            pushExpression(generatorAdapter, context, field.instance(), owner);
            TypeDef fieldType = field.type();
            generatorAdapter.getField(TypeUtils.getType(owner, context.objectDef), field.name(), TypeUtils.getType(fieldType, context.objectDef));
            return;
        }
        if (variableDef instanceof VariableDef.This) {
            if (context.objectDef == null) {
                throw new IllegalStateException("Accessing 'this' is not available");
            }
            generatorAdapter.loadThis();
            return;
        }
        if (variableDef instanceof VariableDef.Super) {
            if (context.objectDef == null) {
                throw new IllegalStateException("Accessing 'super' is not available");
            }
            generatorAdapter.loadThis();
            return;
        }
        throw new UnsupportedOperationException("Unrecognized variable: " + variableDef);
    }

    private void pushConstant(GeneratorAdapter generatorAdapter,
                              ExpressionDef.Constant constant,
                              @Nullable ObjectDef objectDef) {
        TypeDef type = constant.type();
        Object value = constant.value();
        if (value == null) {
            generatorAdapter.push((String) null);
            return;
        }
        if (type instanceof TypeDef.Primitive primitive) {
            switch (primitive.name()) {
                case "long" -> generatorAdapter.push((long) value);
                case "float" -> generatorAdapter.push((float) value);
                case "double" -> generatorAdapter.push((double) value);
                case "boolean" -> generatorAdapter.push((boolean) value);
                case "byte" -> generatorAdapter.push((byte) value);
                case "int" -> generatorAdapter.push((int) value);
                case "short" -> generatorAdapter.push((short) value);
                default ->
                    throw new IllegalStateException("Unrecognized primitive type: " + primitive.name());
            }
            return;
        }
        if (value instanceof String string) {
            generatorAdapter.push(string);
            return;
        }
        if (value instanceof Boolean aBoolean) {
            generatorAdapter.push(aBoolean);
            generatorAdapter.valueOf(Type.getType(boolean.class));
            return;
        }
        if (value instanceof Enum<?> enumConstant) {
            Type enumType = Type.getType(enumConstant.getDeclaringClass());
            generatorAdapter.getStatic(enumType, enumConstant.name(), enumType);
            return;
        }
        if (value instanceof TypeDef typeDef) {
            generatorAdapter.push(TypeUtils.getType(typeDef, objectDef));
            return;
        }
        if (value instanceof Class<?> aClass) {
            generatorAdapter.push(Type.getType(aClass));
            return;
        }
        if (value instanceof Integer integer) {
            generatorAdapter.push(integer);
            generatorAdapter.valueOf(Type.getType(int.class));
            return;
        }
        if (value instanceof Long aLong) {
            generatorAdapter.push(aLong);
            generatorAdapter.valueOf(Type.getType(long.class));
            return;
        }
        if (value instanceof Double aDouble) {
            generatorAdapter.push(aDouble);
            generatorAdapter.valueOf(Type.getType(double.class));
            return;
        }
        if (value instanceof Float aFloat) {
            generatorAdapter.push(aFloat);
            generatorAdapter.valueOf(Type.getType(float.class));
            return;
        }
        if (value instanceof Character character) {
            generatorAdapter.push(character);
            generatorAdapter.valueOf(Type.getType(char.class));
            return;
        }
        if (value instanceof Short aShort) {
            generatorAdapter.push(aShort);
            generatorAdapter.valueOf(Type.getType(short.class));
            return;
        }
        if (value instanceof Byte aByte) {
            generatorAdapter.push(aByte);
            generatorAdapter.valueOf(Type.getType(byte.class));
            return;
        }
        throw new UnsupportedOperationException("Unrecognized constant: " + constant);
    }

    private int getModifiersFlag(Set<Modifier> modifiers) {
        int access = 0;
        if (modifiers.contains(Modifier.PUBLIC)) {
            access |= ACC_PUBLIC;
        }
        if (modifiers.contains(Modifier.PRIVATE)) {
            access |= ACC_PRIVATE;
        }
        if (modifiers.contains(Modifier.PROTECTED)) {
            access |= ACC_PROTECTED;
        }
        if (modifiers.contains(Modifier.FINAL)) {
            access |= ACC_FINAL;
        }
        if (modifiers.contains(Modifier.ABSTRACT)) {
            access |= ACC_ABSTRACT;
        }
        if (modifiers.contains(Modifier.STATIC)) {
            access |= ACC_STATIC;
        }
        return access;
    }

    /**
     * Writes the bytecode of generated class.
     *
     * @param objectDef The object definition.
     * @return The bytes
     */
    public byte[] write(ObjectDef objectDef) {
        return generateClassBytes(objectDef).toByteArray();
    }

    private static String getConstructorDescriptor(@Nullable ObjectDef objectDef, Collection<TypeDef> types) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');

        for (TypeDef argumentType : types) {
            builder.append(TypeUtils.getType(argumentType, objectDef).getDescriptor());
        }

        return builder.append(")V").toString();
    }

    private static String getMethodDescriptor(@Nullable ObjectDef objectDef, MethodDef methodDef) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        for (ParameterDef parameterDef : methodDef.getParameters()) {
            builder.append(TypeUtils.getType(parameterDef.getType(), objectDef));
        }
        builder.append(')');
        builder.append(TypeUtils.getType(Objects.requireNonNullElse(methodDef.getReturnType(), TypeDef.VOID), objectDef));
        return builder.toString();
    }

    private void tableSwitch(final GeneratorAdapter generatorAdapter,
                             final Context context,
                             final Map<Integer, StatementDef> cases,
                             @Nullable final StatementDef defaultCase,
                             @Nullable Runnable finallyBlock) {
        final int[] keys = cases.keySet().stream().sorted().mapToInt(i -> i).toArray();
        float density;
        if (keys.length == 0) {
            density = 0;
        } else {
            density = (float) keys.length / (keys[keys.length - 1] - keys[0] + 1);
        }
        tableSwitch(generatorAdapter, context, keys, cases, defaultCase, finallyBlock, density >= 0.5f);
    }

    private void tableSwitch(final GeneratorAdapter generatorAdapter,
                             final Context context,
                             int[] keys,
                             final Map<Integer, StatementDef> cases,
                             @Nullable final StatementDef defaultCase,
                             @Nullable Runnable finallyBlock,
                             final boolean useTable) {
        Label defaultLabel = generatorAdapter.newLabel();
        Label endLabel = generatorAdapter.newLabel();
        if (keys.length > 0) {
            int numKeys = keys.length;
            if (useTable) {
                int min = keys[0];
                int max = keys[numKeys - 1];
                int range = max - min + 1;
                Label[] labels = new Label[range];
                Arrays.fill(labels, defaultLabel);
                List<Map.Entry<Label, StatementDef>> result = new ArrayList<>();
                for (int key : keys) {
                    int i = key - min;
                    StatementDef statementDef = cases.get(key);
                    Label existingLabel = findIndex(result, statementDef);
                    if (existingLabel == null) {
                        Label newLabel = generatorAdapter.newLabel();
                        labels[i] = newLabel;
                        result.add(Map.entry(newLabel, statementDef));
                    } else {
                        // Reuse the label
                        labels[i] = existingLabel;
                    }
                }
                generatorAdapter.visitTableSwitchInsn(min, max, defaultLabel, labels);
                for (Map.Entry<Label, StatementDef> e : result) {
                    generatorAdapter.mark(e.getKey());
                    pushStatement(generatorAdapter, context, e.getValue(), finallyBlock);
                    generatorAdapter.goTo(endLabel);
                }
            } else {
                Label[] labels = new Label[keys.length];
                List<Map.Entry<Label, StatementDef>> result = new ArrayList<>();
                for (int i = 0; i < numKeys; ++i) {
                    int key = keys[i];
                    StatementDef statementDef = cases.get(key);
                    Label existingLabel = findIndex(result, statementDef);
                    if (existingLabel == null) {
                        Label newLabel = generatorAdapter.newLabel();
                        labels[i] = newLabel;
                        result.add(Map.entry(newLabel, statementDef));
                    } else {
                        // Reuse the label
                        labels[i] = existingLabel;
                    }
                }
                generatorAdapter.visitLookupSwitchInsn(defaultLabel, keys, labels);
                for (Map.Entry<Label, StatementDef> e : result) {
                    generatorAdapter.mark(e.getKey());
                    pushStatement(generatorAdapter, context, e.getValue(), finallyBlock);
                    generatorAdapter.goTo(endLabel);
                }
            }
        }
        generatorAdapter.mark(defaultLabel);
        if (defaultCase != null) {
            pushStatement(generatorAdapter, context, defaultCase, finallyBlock);
        }
        generatorAdapter.mark(endLabel);
    }

    private static Label findIndex(List<Map.Entry<Label, StatementDef>> result, StatementDef statement) {
        for (Map.Entry<Label, StatementDef> e : result) {
            if (e.getValue() == statement) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * The statement context.
     *
     * @param objectDef The current object definition
     * @param methodDef The current method definition.
     * @param locals    The locals
     */
    public record Context(@Nullable ObjectDef objectDef,
                          MethodDef methodDef,
                          Map<String, Integer> locals) {

        public Context(@Nullable ObjectDef objectDef,
                       MethodDef methodDef) {
            this(objectDef, methodDef, new HashMap<>());
        }

    }

    private static final class CatchBlock {

        private final StatementDef.Try.Catch aCatch;
        private final Label from;
        private Label to;

        private CatchBlock(StatementDef.Try.Catch aCatch, Label from) {
            this.aCatch = aCatch;
            this.from = from;
        }
    }

}
