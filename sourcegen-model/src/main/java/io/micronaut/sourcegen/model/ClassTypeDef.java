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
package io.micronaut.sourcegen.model;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * The class type definition.
 * Not-null by default.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public sealed interface ClassTypeDef extends TypeDef {

    ClassTypeDef OBJECT = of(Object.class);

    /**
     * @return The type name
     */
    String getName();

    /**
     * @return The type name
     * @since 1.5
     */
    String getCanonicalName();

    /**
     * @return The simple name
     */
    default String getSimpleName() {
        return NameUtils.getSimpleName(getName());
    }

    /**
     * @return The package name
     */
    default String getPackageName() {
        return NameUtils.getPackageName(getName());
    }

    @Override
    ClassTypeDef makeNullable();

    /**
     * @return True if the class is an enum
     * @since 1.2
     */
    default boolean isEnum() {
        return false;
    }

    /**
     * @return True if interface
     * @since 1.5
     */
    default boolean isInterface() {
        return false;
    }

    /**
     * @return True if inner
     * @since 1.5
     */
    default boolean isInner() {
        return false;
    }

    /**
     * The new instance expression.
     *
     * @param values The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(ExpressionDef... values) {
        return instantiate(List.of(values));
    }

    /**
     * The new instance expression.
     *
     * @param values The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(List<? extends ExpressionDef> values) {
        return instantiate(values.stream().map(ExpressionDef::type).toList(), values);
    }

    /**
     * The new instance expression.
     *
     * @param parameterTypes The constructor parameter types
     * @param values         The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(List<TypeDef> parameterTypes, ExpressionDef... values) {
        return instantiate(parameterTypes, List.of(values));
    }

    /**
     * The new instance expression.
     *
     * @param parameterTypes The constructor parameter types
     * @param values         The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(List<TypeDef> parameterTypes, List<? extends ExpressionDef> values) {
        return new ExpressionDef.NewInstance(this, parameterTypes, values);
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(Constructor<?> constructor, ExpressionDef... values) {
        return instantiate(constructor, List.of(values));
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(Constructor<?> constructor, List<? extends ExpressionDef> values) {
        return instantiate(Arrays.stream(constructor.getParameterTypes()).map(TypeDef::of).toList(), values);
    }

    /**
     * The new instance expression.
     *
     * @param methodElement The method element
     * @param values        The constructor values
     * @return The new instance
     */
    @Experimental
    default ExpressionDef.NewInstance instantiate(MethodElement methodElement, List<? extends ExpressionDef> values) {
        return instantiate(Arrays.stream(methodElement.getSuspendParameters()).map(ParameterElement::getType).map(TypeDef::erasure).toList(), values);
    }

    /**
     * Get static field.
     *
     * @param name The field name
     * @param type The field type
     * @return the get static field expression
     * @since 1.5
     */
    default VariableDef.StaticField getStaticField(String name,
                                                   TypeDef type) {
        return new VariableDef.StaticField(this, name, type);
    }

    /**
     * Get static field.
     *
     * @param field The field
     * @return the get static field expression
     * @since 1.5
     */
    default VariableDef.StaticField getStaticField(FieldDef field) {
        return getStaticField(field.getName(), field.getType());
    }

    /**
     * Get static field.
     *
     * @param field The field
     * @return the get static field expression
     * @since 1.5
     */
    default VariableDef.StaticField getStaticField(Field field) {
        return getStaticField(field.getName(), TypeDef.of(field.getType()));
    }

    /**
     * Invoke static method.
     *
     * @param name          The method name
     * @param returningType The return type
     * @param values        The values
     * @return the invoke static method expression
     * @since 1.2
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(String name,
                                                          TypeDef returningType,
                                                          List<? extends ExpressionDef> values) {
        return invokeStatic(name, values.stream().map(ExpressionDef::type).toList(), returningType, values);
    }

    /**
     * Invoke static method.
     *
     * @param name           The method name
     * @param parameterTypes The parameter types
     * @param returningType  The return type
     * @param values         The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(String name,
                                                          List<TypeDef> parameterTypes,
                                                          TypeDef returningType,
                                                          List<? extends ExpressionDef> values) {
        return new ExpressionDef.InvokeStaticMethod(this,
            MethodDef.builder(name)
                .addParameters(parameterTypes)
                .returns(returningType)
                .build(),
            values);
    }

    /**
     * Invoke static method.
     *
     * @param name          The method name
     * @param returningType The return type
     * @param values        The parameters
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(String name,
                                                          TypeDef returningType,
                                                          ExpressionDef... values) {
        return invokeStatic(name, returningType, List.of(values));
    }

    /**
     * Invoke static method.
     *
     * @param name          The method name
     * @param parameterTypes The parameter types
     * @param returningType The return type
     * @param values    The parameters
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(String name,
                                                          List<TypeDef> parameterTypes,
                                                          TypeDef returningType,
                                                          ExpressionDef... values) {
        return invokeStatic(name, parameterTypes, returningType, List.of(values));
    }

    /**
     * Invoke static method.
     *
     * @param method The method
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(MethodDef method, ExpressionDef... values) {
        return invokeStatic(method, List.of(values));
    }

    /**
     * Invoke static method.
     *
     * @param method The method
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(Method method, ExpressionDef... values) {
        return invokeStatic(method, List.of(values));
    }

    /**
     * Invoke static method.
     *
     * @param method The method
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(Method method, List<? extends ExpressionDef> values) {
        return invokeStatic(
            method.getName(),
            Arrays.stream(method.getParameters()).map(p -> TypeDef.of(p.getType())).toList(),
            TypeDef.of(method.getReturnType()),
            values);
    }

    /**
     * Invoke static method.
     *
     * @param methodElement The method element
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(MethodElement methodElement, ExpressionDef... values) {
        return invokeStatic(methodElement, List.of(values));
    }

    /**
     * Invoke static method.
     *
     * @param methodElement The method element
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(MethodElement methodElement, List<? extends ExpressionDef> values) {
        return invokeStatic(
            methodElement.getName(),
            Arrays.stream(methodElement.getSuspendParameters()).map(p -> TypeDef.erasure(p.getType())).toList(),
            methodElement.isSuspend() ? TypeDef.OBJECT : TypeDef.of(methodElement.getReturnType()),
            values
        );
    }

    /**
     * Invoke static method.
     *
     * @param method The method
     * @param values The values
     * @return the invoke static method expression
     * @since 1.5
     */
    default ExpressionDef.InvokeStaticMethod invokeStatic(MethodDef method, List<? extends ExpressionDef> values) {
        return new ExpressionDef.InvokeStaticMethod(this, method, values);
    }

    /**
     * Create a new type definition.
     *
     * @param type The class
     * @return type definition
     */
    static ClassTypeDef of(Class<?> type) {
        if (type.isPrimitive()) {
            throw new IllegalStateException("Primitive classes cannot be of type: " + ClassTypeDef.class.getName());
        }
        return new JavaClass(type, false);
    }

    /**
     * Create a new type definition.
     *
     * @param className The class name
     * @return type definition
     */
    static ClassTypeDef of(String className) {
        return of(className, false);
    }

    /**
     * Create a new type definition.
     *
     * @param className The class name
     * @param isInner   Is inner type
     * @return type definition
     * @since 1.5
     */
    static ClassTypeDef of(String className, boolean isInner) {
        return new ClassName(className, isInner, false);
    }

    /**
     * Create a new type definition.
     *
     * @param classElement The class element
     * @return type definition
     */
    static ClassTypeDef of(ClassElement classElement) {
        if (classElement.isPrimitive()) {
            throw new IllegalStateException("Primitive classes cannot be of type: " + ClassTypeDef.class.getName());
        }
        return new ClassElementType(classElement, classElement.isNullable());
    }

    /**
     * Create a new type definition.
     *
     * @param classDef The class definition
     * @return type definition
     */
    static ClassTypeDef of(ClassDef classDef) {
        return new ClassDefType(classDef, false);
    }

    /**
     * Define a ClassTypeDef with annotations.
     *
     * @param annotations the annotation definitions to be added
     * @return The AnnotatedClassTypeDef
     * @since 1.4
     */
    @Override
    default AnnotatedClassTypeDef annotated(AnnotationDef... annotations) {
        return annotated(List.of(annotations));
    }

    /**
     * Define a ClassTypeDef with annotations.
     *
     * @param annotations The list of the AnnotationDef
     * @return The AnnotatedClassTypeDef
     * @since 1.4
     */
    @Override
    default AnnotatedClassTypeDef annotated(List<AnnotationDef> annotations) {
        return new AnnotatedClassTypeDef(this, annotations);
    }

    /**
     * The class type.
     *
     * @param type     The type
     * @param nullable Is nullable
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record JavaClass(Class<?> type, boolean nullable) implements ClassTypeDef {

        @Override
        public String getName() {
            return type.getName();
        }

        @Override
        public String getCanonicalName() {
            return type.getCanonicalName();
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }

        @Override
        public ClassTypeDef makeNullable() {
            return new JavaClass(type, true);
        }

        @Override
        public boolean isEnum() {
            return type.isEnum();
        }

        @Override
        public boolean isInterface() {
            return type.isInterface();
        }

        @Override
        public boolean isInner() {
            return type.isMemberClass();
        }
    }

    /**
     * The class name type.
     *
     * @param className The class name
     * @param isInner   Is inner
     * @param nullable  Is nullable
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record ClassName(String className, boolean isInner, boolean nullable) implements ClassTypeDef {

        @Override
        public String getName() {
            return className;
        }

        @Override
        public String getCanonicalName() {
            if (isInner) {
                return className.replace("$", ".");
            }
            return className;
        }

        @Override
        public boolean isInner() {
            return isInner;
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }

        @Override
        public ClassTypeDef makeNullable() {
            return new ClassName(className,  isInner, true);
        }

    }

    /**
     * The class element type.
     *
     * @param classElement The class element
     * @param nullable     Is nullable
     * @author Denis Stepanov
     * @since 1.2
     */
    @Experimental
    record ClassElementType(ClassElement classElement, boolean nullable) implements ClassTypeDef {

        @Override
        public String getName() {
            return classElement.getName();
        }

        @Override
        public String getCanonicalName() {
            return classElement.getCanonicalName();
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }

        @Override
        public ClassTypeDef makeNullable() {
            return new ClassElementType(classElement, true);
        }

        @Override
        public boolean isEnum() {
            return classElement.isEnum();
        }

        @Override
        public boolean isInterface() {
            return classElement.isInterface();
        }

        @Override
        public boolean isInner() {
            return classElement.isInner();
        }
    }

    /**
     * The class def element type.
     *
     * @param classDef The class def
     * @param nullable Is nullable
     * @author Denis Stepanov
     * @since 1.2
     */
    @Experimental
    record ClassDefType(ClassDef classDef, boolean nullable) implements ClassTypeDef {

        @Override
        public String getName() {
            return classDef.getName();
        }

        @Override
        public String getCanonicalName() {
            return classDef.getName().replace("$", ".");
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }

        @Override
        public ClassTypeDef makeNullable() {
            return new ClassDefType(classDef, true);
        }

    }

    /**
     * The parameterized type definition.
     *
     * @param rawType       The raw type definition
     * @param typeArguments The type arguments
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record Parameterized(ClassTypeDef rawType,
                         List<TypeDef> typeArguments) implements ClassTypeDef {
        @Override
        public String getName() {
            return rawType.getName();
        }

        @Override
        public String getCanonicalName() {
            return rawType.getCanonicalName();
        }

        @Override
        public boolean isNullable() {
            return rawType.isNullable();
        }

        @Override
        public ClassTypeDef makeNullable() {
            return new Parameterized(rawType.makeNullable(), typeArguments);
        }

        @Override
        public boolean isInner() {
            return rawType.isInner();
        }

        @Override
        public boolean isInterface() {
            return rawType.isInterface();
        }

    }

    /**
     * A combined type for representing a ClassTypeDef with annotations.
     *
     * @param typeDef       The raw type definition
     * @param annotations   List of annotations to associate
     * @author Elif Kurtay
     * @since 1.4
     */
    @Experimental
    record AnnotatedClassTypeDef(ClassTypeDef typeDef,
                                 List<AnnotationDef> annotations) implements TypeDef.Annotated {
    }

}
