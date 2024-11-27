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

import com.github.javaparser.quality.NotNull;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The expression definition.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public sealed interface ExpressionDef
    permits ExpressionDef.Cast, ExpressionDef.ConditionExpressionDef, ExpressionDef.Constant, ExpressionDef.ArrayElement, ExpressionDef.GetPropertyValue, ExpressionDef.IfElse, ExpressionDef.InstanceOf, ExpressionDef.InvokeGetClassMethod, ExpressionDef.InvokeHashCodeMethod, ExpressionDef.InvokeInstanceMethod, ExpressionDef.InvokeStaticMethod, ExpressionDef.MathOp, ExpressionDef.NewArrayInitialized, ExpressionDef.NewArrayOfSize, ExpressionDef.NewInstance, ExpressionDef.Switch, ExpressionDef.SwitchYieldCase, VariableDef {

    /**
     * Check an array element.
     *
     * @param index The index
     * @return The array element
     * @since 1.5
     */
    default ArrayElement arrayElement(int index) {
        return new ArrayElement(this, index);
    }

    /**
     * Check if the instance is of the type.
     *
     * @param instanceType The instance type
     * @return The instance of expression
     * @since 1.5
     */
    default ExpressionDef.InstanceOf instanceOf(ClassTypeDef instanceType) {
        return new ExpressionDef.InstanceOf(this, instanceType);
    }

    /**
     * Throw an exception.
     *
     * @return The throw statement
     */
    default StatementDef.Throw doThrow() {
        return new StatementDef.Throw(this);
    }

    /**
     * The condition of this variable.
     *
     * @param op         The operator
     * @param expression The expression of this variable
     * @return The condition expression
     * @since 1.2
     */
    default ExpressionDef asCondition(String op, ExpressionDef expression) {
        return new ExpressionDef.Condition(op, this, expression);
    }

    /**
     * The math operation of this variable.
     *
     * @param op         The operator
     * @param expression The expression of this variable
     * @return The condition expression
     * @since 1.2
     */
    default ExpressionDef math(String op, ExpressionDef expression) {
        return new ExpressionDef.MathOp(op, this, expression);
    }

    /**
     * The and condition of this variable.
     *
     * @param expression The expression of this variable
     * @return The "and" condition expression
     * @since 1.3
     */
    default ExpressionDef asConditionAnd(ExpressionDef expression) {
        return new ExpressionDef.And(this, expression);
    }

    /**
     * The or condition of this variable.
     *
     * @param expression The expression of this variable
     * @return The "or" condition expression
     * @since 1.3
     */
    default ExpressionDef asConditionOr(ExpressionDef expression) {
        return new ExpressionDef.Or(this, expression);
    }

    /**
     * @return Is non-null expression
     * @since 1.2
     */
    default ExpressionDef isNonNull() {
        return new ExpressionDef.IsNotNull(this);
    }

    /**
     * Is not null - if / else expression.
     *
     * @param ifExpression   If expression
     * @param elseExpression Else expression
     * @return Is not null expression
     * @since 1.5
     */
    default ExpressionDef isNonNull(ExpressionDef ifExpression, ExpressionDef elseExpression) {
        return isNonNull().asConditionIfElse(ifExpression, elseExpression);
    }

    /**
     * Is not null - if statement.
     *
     * @param ifStatement If statement
     * @return Is not null statement
     * @since 1.5
     */
    default StatementDef isNonNull(StatementDef ifStatement) {
        return isNonNull().asConditionIf(ifStatement);
    }

    /**
     * Is not null - if / else statement.
     *
     * @param ifStatement   If statement
     * @param elseStatement Else statement
     * @return Is not null statement
     * @since 1.5
     */
    default StatementDef isNonNull(StatementDef ifStatement, StatementDef elseStatement) {
        return isNonNull().asConditionIfElse(ifStatement, elseStatement);
    }

    /**
     * @return Is null expression
     * @since 1.2
     */
    default ExpressionDef isNull() {
        return new ExpressionDef.IsNull(this);
    }

    /**
     * Is null - if / else expression.
     *
     * @param ifExpression   If expression
     * @param elseExpression Else expression
     * @return Is null expression
     * @since 1.5
     */
    default ExpressionDef isNull(ExpressionDef ifExpression, ExpressionDef elseExpression) {
        return isNull().asConditionIfElse(ifExpression, elseExpression);
    }

    /**
     * Is null - if statement.
     *
     * @param ifStatement If statement
     * @return Is null statement
     * @since 1.5
     */
    default StatementDef isNull(StatementDef ifStatement) {
        return isNull().asConditionIf(ifStatement);
    }

    /**
     * Is null - if / else statement.
     *
     * @param ifStatement   If statement
     * @param elseStatement Else statement
     * @return Is null statement
     * @since 1.5
     */
    default StatementDef isNull(StatementDef ifStatement, StatementDef elseStatement) {
        return isNull().asConditionIfElse(ifStatement, elseStatement);
    }

    /**
     * @return Is true expression
     * @since 1.5
     */
    default ExpressionDef.ConditionExpressionDef isTrue() {
        return new ExpressionDef.Condition("==", trueValue(), this);
    }

    /**
     * @return Is false expression
     * @since 1.5
     */
    default ExpressionDef.ConditionExpressionDef isFalse() {
        return new ExpressionDef.Condition("==", falseValue(), this);
    }

    /**
     * @return The null value expression
     * @since 1.2
     */
    @NonNull
    static ExpressionDef.Constant nullValue() {
        return new Constant(TypeDef.OBJECT, null);
    }

    /**
     * @return The true value expression
     * @since 1.2
     */
    @NonNull
    static ExpressionDef.Constant trueValue() {
        return TypeDef.Primitive.TRUE;
    }

    /**
     * @return The true value expression
     * @since 1.2
     */
    @NonNull
    static ExpressionDef.Constant falseValue() {
        return TypeDef.Primitive.FALSE;
    }

    /**
     * The statement returning this expression.
     *
     * @return The statement returning this expression
     */
    default StatementDef returning() {
        return new StatementDef.Return(this);
    }

    /**
     * Cast expression to a different type.
     *
     * @param type The type to cast to
     * @return The cast expression
     */
    @NonNull
    default ExpressionDef.Cast cast(TypeDef type) {
        return new Cast(type, this);
    }

    /**
     * Cast expression to a different type.
     *
     * @param type The type to cast to
     * @return The cast expression
     * @since 1.5
     */
    @NonNull
    default ExpressionDef.Cast cast(Class<?> type) {
        return new Cast(TypeDef.of(type), this);
    }

    /**
     * The conditional statement based on this expression.
     *
     * @param statement The statement
     * @return The statement returning this expression
     * @since 1.2
     */
    default StatementDef asConditionIf(StatementDef statement) {
        return new StatementDef.If(this, statement);
    }

    /**
     * The conditional statement based on this expression.
     *
     * @param statement     The statement
     * @param elseStatement The else statement
     * @return The statement returning this expression
     * @since 1.2
     */
    default StatementDef asConditionIfElse(StatementDef statement, StatementDef elseStatement) {
        return new StatementDef.IfElse(this, statement, elseStatement);
    }

    /**
     * The conditional if else expression.
     *
     * @param expression     The expression
     * @param elseExpression The else expression
     * @return The statement returning this expression
     * @since 1.2
     */
    default ExpressionDef asConditionIfElse(ExpressionDef expression, ExpressionDef elseExpression) {
        return new ExpressionDef.IfElse(this, expression, elseExpression);
    }

    /**
     * Turn this expression into a new local variable.
     *
     * @param name The local name
     * @return A new local
     * @since 1.2
     */
    default StatementDef.DefineAndAssign newLocal(String name) {
        return new VariableDef.Local(name, type()).defineAndAssign(this);
    }

    /**
     * Turn this expression into a new local variable.
     *
     * @param name The local name
     * @param fn   The contextual function
     * @return A new local
     * @since 1.2
     */
    default StatementDef newLocal(String name, Function<VariableDef, StatementDef> fn) {
        StatementDef.DefineAndAssign defineAndAssign = newLocal(name);
        return StatementDef.multi(
            defineAndAssign,
            fn.apply(defineAndAssign.variable())
        );
    }

    /**
     * Turn this expression into a statement switch.
     *
     * @param type  The expression type
     * @param cases The cases
     * @return A new switch expression
     * @since 1.2
     */
    default StatementDef asStatementSwitch(TypeDef type, Map<Constant, StatementDef> cases) {
        return asStatementSwitch(type, cases, null);
    }

    /**
     * Turn this expression into an expression switch.
     *
     * @param type        The expression type
     * @param cases       The cases
     * @param defaultCase The default cae
     * @return A new switch expression
     * @since 1.5
     */
    default ExpressionDef.Switch asExpressionSwitch(TypeDef type,
                                                    Map<Constant, ? extends ExpressionDef> cases,
                                                    ExpressionDef defaultCase) {
        if (defaultCase == null) {
            cases = new HashMap<>(cases);
            defaultCase = cases.remove(nullValue());
            if (defaultCase == null) {
                defaultCase = cases.remove(null);
                if (defaultCase == null) {
                    throw new IllegalStateException("The expression switch requires a default expression");
                }
            }
        }
        return new Switch(this, type, cases, defaultCase);
    }

    /**
     * Turn this expression into a statement switch.
     *
     * @param type  The expression type
     * @param cases The cases
     * @param defaultCase The default case
     * @return A new switch expression
     * @since 1.2
     */
    default StatementDef.Switch asStatementSwitch(TypeDef type,
                                                  Map<Constant, StatementDef> cases,
                                                  StatementDef defaultCase) {
        if (defaultCase == null) {
            cases = new HashMap<>(cases);
            defaultCase = cases.remove(nullValue());
            if (defaultCase == null) {
                defaultCase = cases.remove(null);
            }
        }
        return new StatementDef.Switch(this, type, cases, defaultCase);
    }

    /**
     * Turn this expression into a while statement.
     *
     * @param statement The statement
     * @return A new switch expression
     * @since 1.2
     */
    default StatementDef.While whileLoop(StatementDef statement) {
        return new StatementDef.While(this, statement);
    }

    /**
     * Reference the field of this variable.
     *
     * @param fieldName The field type
     * @param typeDef   Teh field type
     * @return The field variable
     * @since 1.2
     */
    default VariableDef.Field field(String fieldName, TypeDef typeDef) {
        return new VariableDef.Field(this, fieldName, typeDef);
    }

    /**
     * Reference the field of this variable.
     *
     * @param fieldDef The field definition
     * @return The field variable
     * @since 1.2
     */
    default VariableDef.Field field(FieldDef fieldDef) {
        return new VariableDef.Field(this, fieldDef.name, fieldDef.getType());
    }

    /**
     * Reference the field of this variable.
     *
     * @param fieldElement The field definition
     * @return The field variable
     * @since 1.5
     */
    default VariableDef.Field field(FieldElement fieldElement) {
        return new VariableDef.Field(this, fieldElement.getName(), TypeDef.of(fieldElement.getType()));
    }

    /**
     * The invoke constructor expression.
     *
     * @param values The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invokeConstructor(ExpressionDef... values) {
        return invokeConstructor(Arrays.asList(values));
    }

    /**
     * The invoke constructor expression.
     *
     * @param values The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invokeConstructor(List<? extends ExpressionDef> values) {
        return invokeConstructor(values.stream().map(ExpressionDef::type).toList(), values);
    }

    /**
     * The invoke constructor expression.
     *
     * @param parameterTypes The parameterTypes
     * @param values         The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invokeConstructor(List<TypeDef> parameterTypes, ExpressionDef... values) {
        return invokeConstructor(parameterTypes, Arrays.asList(values));
    }

    /**
     * The invoke constructor expression.
     *
     * @param parameterTypes The parameterTypes
     * @param values         The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invokeConstructor(List<TypeDef> parameterTypes, List<? extends ExpressionDef> values) {
        return new InvokeInstanceMethod(this, MethodDef.constructor().addParameters(parameterTypes).build(), values);
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default InvokeInstanceMethod invokeConstructor(Constructor<?> constructor, ExpressionDef... values) {
        return invokeConstructor(constructor, List.of(values));
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default InvokeInstanceMethod invokeConstructor(Constructor<?> constructor, List<? extends ExpressionDef> values) {
        return invokeConstructor(Arrays.stream(constructor.getParameterTypes()).map(TypeDef::of).toList(), values);
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default InvokeInstanceMethod invokeConstructor(MethodDef constructor, ExpressionDef... values) {
        return invokeConstructor(constructor, List.of(values));
    }

    /**
     * The new instance expression.
     *
     * @param constructor The constructor
     * @param values      The constructor values
     * @return The new instance
     */
    @Experimental
    default InvokeInstanceMethod invokeConstructor(MethodDef constructor, List<? extends ExpressionDef> values) {
        return invokeConstructor(constructor.getParameters().stream().map(ParameterDef::getType).toList(), values);
    }

    /**
     * The call the instance method expression.
     *
     * @param method The method
     * @param values The values
     * @return The call to the instance method
     * @since 1.2
     */
    default InvokeInstanceMethod invoke(MethodDef method, ExpressionDef... values) {
        return invoke(method, List.of(values));
    }

    /**
     * The call the instance method expression.
     *
     * @param methodDef The method
     * @param values    The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invoke(MethodDef methodDef, List<? extends ExpressionDef> values) {
        return new InvokeInstanceMethod(this, methodDef, values);
    }

    /**
     * The invoke the method defined by the reflection.
     *
     * @param method The method
     * @param values The parameters
     * @return The invoke method expression
     * @since 1.5
     */
    default InvokeInstanceMethod invoke(Method method, ExpressionDef... values) {
        return invoke(method, Arrays.asList(values));
    }

    /**
     * The invoke the method defined by the reflection.
     *
     * @param method The method
     * @param values The parameters
     * @return The invoke method expression
     * @since 1.5
     */
    default InvokeInstanceMethod invoke(Method method, List<? extends ExpressionDef> values) {
        return new InvokeInstanceMethod(
            this,
            MethodDef.of(method),
            method.isDefault(),
            values
        );
    }

    /**
     * The call the instance method expression.
     *
     * @param name      The method name
     * @param returning The returning
     * @param values    The parameters
     * @return The call to the instance method
     * @since 1.2
     */
    default InvokeInstanceMethod invoke(String name, TypeDef returning, ExpressionDef... values) {
        return invoke(name, returning, List.of(values));
    }

    /**
     * The call the instance method expression.
     *
     * @param name      The method name
     * @param returning The returning
     * @param values    The values
     * @return The call to the instance method
     * @since 1.2
     */
    default InvokeInstanceMethod invoke(String name, TypeDef returning, List<? extends ExpressionDef> values) {
        return invoke(
            name,
            values.stream().map(ExpressionDef::type).toList(),
            returning,
            values
        );
    }

    /**
     * The call the instance method expression.
     *
     * @param name           The method name
     * @param parameterTypes The parameterTypes
     * @param returning      The returning
     * @param values         The values
     * @return The call to the instance method
     * @since 1.5
     */
    default InvokeInstanceMethod invoke(String name, List<TypeDef> parameterTypes, TypeDef returning, List<? extends ExpressionDef> values) {
        return new InvokeInstanceMethod(
            this,
            MethodDef.builder(name).addParameters(parameterTypes).returns(returning).build(),
            values
        );
    }

    /**
     * The call the instance method expression.
     *
     * @param methodElement The method element
     * @param values        The values
     * @return The call to the instance method
     * @since 1.2
     */
    default InvokeInstanceMethod invoke(MethodElement methodElement, ExpressionDef... values) {
        return invoke(methodElement, List.of(values));
    }

    /**
     * The call the instance method expression.
     *
     * @param methodElement The method element
     * @param values        The parameters
     * @return The call to the instance method
     * @since 1.2
     */
    default InvokeInstanceMethod invoke(MethodElement methodElement, List<? extends ExpressionDef> values) {
        return new InvokeInstanceMethod(
            this,
            MethodDef.of(methodElement),
            methodElement.isDefault(),
            values
        );
    }

    /**
     * The invocation of the {@link Object#hashCode()} or equivalent method for the expression.
     *
     * @return The hash code invocation
     * @since 1.2
     */
    default InvokeHashCodeMethod invokeHashCode() {
        return new InvokeHashCodeMethod(this);
    }

    /**
     * The invocation of the {@link Object#getClass()}} or equivalent method for the expression.
     *
     * @return The get class invocation
     * @since 1.2
     */
    default InvokeGetClassMethod invokeGetClass() {
        return new InvokeGetClassMethod(this);
    }

    /**
     * The structurally equals {@link Object#equals(Object)} of this expression and the other expression.
     *
     * @param other The other expression to compare with
     * @return The equals expression
     * @since 1.3
     */
    default EqualsStructurally equalsStructurally(ExpressionDef other) {
        return new EqualsStructurally(this, other);
    }

    /**
     * The referentially equals (==) of this expression and the other expression.
     *
     * @param other The other expression to compare with
     * @return The equals expression
     * @since 1.3
     */
    default EqualsReferentially equalsReferentially(ExpressionDef other) {
        return new EqualsReferentially(this, other);
    }

    /**
     * The get property value expression.
     *
     * @param propertyElement The property element
     * @return The get property value expression
     * @since 1.3
     */
    default GetPropertyValue getPropertyValue(PropertyElement propertyElement) {
        return new GetPropertyValue(this, propertyElement);
    }

    /**
     * Resolve a constant for the given type from the string.
     *
     * @param type    The type
     * @param typeDef The type def
     * @param value   The string value
     * @return The constant
     * @throws IllegalArgumentException if the constant is not supported.
     */
    @Experimental
    @Nullable
    static ExpressionDef constant(ClassElement type, TypeDef typeDef, @Nullable Object value) {
        Objects.requireNonNull(type, "Type cannot be null");
        if (type.isPrimitive()) {
            return ClassUtils.getPrimitiveType(type.getName()).flatMap(t ->
                ConversionService.SHARED.convert(value, t)
            ).map(o -> new Constant(typeDef, o)).orElse(null);
        } else if (ClassUtils.isJavaLangType(type.getName())) {
            return ClassUtils.forName(type.getName(), ExpressionDef.class.getClassLoader())
                .flatMap(t -> ConversionService.SHARED.convert(value, t))
                .map(o -> new Constant(typeDef, o)).orElse(null);
        } else if (type.isEnum()) {
            String name;
            if (value instanceof Enum<?> anEnum) {
                name = anEnum.name();
            } else {
                name = value.toString();
            }
            return ((ClassTypeDef) typeDef).getStaticField(name, typeDef);
        }
        return ExpressionDef.nullValue();
    }

    /**
     * A new constant.
     *
     * @param value The string value
     * @return The constant
     * @throws IllegalArgumentException if the constant is not supported.
     * @since 1.2
     */
    @Experimental
    @Nullable
    static ExpressionDef.Constant constant(@Nullable Object value) {
        if (value == null) {
            return ExpressionDef.nullValue();
        }
        TypeDef type;
        if (value instanceof TypeDef) {
            type = TypeDef.CLASS;
        } else {
            type = TypeDef.of(value.getClass());
        }
        return new Constant(type, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.5
     */
    @Experimental
    static ExpressionDef.Constant constant(boolean value) {
        return new Constant(TypeDef.Primitive.BOOLEAN, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.5
     */
    @Experimental
    static ExpressionDef.Constant constant(int value) {
        return new Constant(TypeDef.Primitive.INT, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.5
     */
    @Experimental
    static ExpressionDef.Constant constant(long value) {
        return new Constant(TypeDef.Primitive.LONG, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.4
     */
    @Experimental
    static ExpressionDef.Constant constant(double value) {
        return new Constant(TypeDef.Primitive.DOUBLE, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.5
     */
    @Experimental
    static ExpressionDef.Constant constant(float value) {
        return new Constant(TypeDef.Primitive.FLOAT, value);
    }

    /**
     * A new constant.
     *
     * @param value The value
     * @return The constant
     * @since 1.5
     */
    @Experimental
    static ExpressionDef.Constant constant(char value) {
        return new Constant(TypeDef.Primitive.CHAR, value);
    }

    /**
     * Resolve a constant for the given type from the string.
     *
     * @param value The string value
     * @return The constant
     * @throws IllegalArgumentException if the constant is not supported.
     * @since 1.2
     */
    @Experimental
    @Nullable
    static ExpressionDef.Constant primitiveConstant(@NotNull Object value) {
        Class<?> primitiveType = ReflectionUtils.getPrimitiveType(value.getClass());
        return new ExpressionDef.Constant(TypeDef.primitive(primitiveType), value);
    }

    /**
     * The type of the expression.
     *
     * @return The type
     */
    TypeDef type();

    /**
     * The new instance expression.
     *
     * @param type           The type
     * @param parameterTypes The parameterTypes
     * @param values         The constructor values
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record NewInstance(ClassTypeDef type,
                       List<TypeDef> parameterTypes,
                       List<? extends ExpressionDef> values) implements ExpressionDef {
    }

    /**
     * The cast expression. No checks are performed on the types and casting expression is
     * always generated.
     *
     * @param type          The type to cast to
     * @param expressionDef The expression to cast
     * @author Andriy Dmytruk
     * @since 1.3
     */
    @Experimental
    record Cast(TypeDef type, ExpressionDef expressionDef) implements ExpressionDef {
    }

    /**
     * The constant expression.
     *
     * @param type  The type
     * @param value The value
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record Constant(TypeDef type,
                    @Nullable
                    Object value) implements ExpressionDef {
    }

    /**
     * The call an instance method expression.
     *
     * @param instance  The instance
     * @param method    The method
     * @param isDefault Is default method
     * @param values    The parameters
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record InvokeInstanceMethod(ExpressionDef instance,
                                MethodDef method,
                                boolean isDefault,
                                List<? extends ExpressionDef> values) implements ExpressionDef, StatementDef {

        public InvokeInstanceMethod(ExpressionDef instance, MethodDef method, List<? extends ExpressionDef> values) {
            this(instance, method, false, values);
        }

        public InvokeInstanceMethod {
            if (method.getParameters().size() != values.size()) {
                throw new IllegalStateException("Method " + method.getName() + " parameters: " + method.getParameters().size() + " doesn't match values provided: " + values.size());
            }
        }

        @Override
        public TypeDef type() {
            return method.getReturnType();
        }
    }

    /**
     * The call a static method expression.
     *
     * @param classDef The class
     * @param method   The method
     * @param values   The values
     * @author Denis Stepanov
     * @since 1.0
     */
    @Experimental
    record InvokeStaticMethod(ClassTypeDef classDef,
                              MethodDef method,
                              List<? extends ExpressionDef> values) implements ExpressionDef, StatementDef {
        @Override
        public TypeDef type() {
            return method.getReturnType();
        }
    }

    /**
     * The condition operator.
     *
     * @param operator The operator
     * @param left     The left expression
     * @param right    The right expression
     * @author Denis Stepanov
     */
    @Experimental
    record Condition(String operator,
                     ExpressionDef left,
                     ExpressionDef right) implements ConditionExpressionDef {
    }

    /**
     * The math operator.
     *
     * @param operator The operator
     * @param left     The left expression
     * @param right    The right expression
     * @author Denis Stepanov
     */
    @Experimental
    record MathOp(String operator,
                  ExpressionDef left,
                  ExpressionDef right) implements ExpressionDef {
        @Override
        public TypeDef type() {
            return left.type();
        }
    }

    /**
     * The IS NULL condition.
     *
     * @param expression The expression
     * @author Denis Stepanov
     */
    @Experimental
    record IsNull(ExpressionDef expression) implements ConditionExpressionDef {
    }

    /**
     * The IS NOT NULL condition.
     *
     * @param expression The expression
     * @author Denis Stepanov
     */
    @Experimental
    record IsNotNull(ExpressionDef expression) implements ConditionExpressionDef {
    }

    /**
     * The and condition. Puts parenthesis around itself when needed.
     *
     * @param left  The left expression
     * @param right The right expression
     * @author Elif Kurtay
     * @since 1.3
     */
    @Experimental
    record And(ExpressionDef left, ExpressionDef right) implements ConditionExpressionDef {
    }

    /**
     * The or condition. Puts parenthesis around itself when needed.
     *
     * @param left  The left expression
     * @param right The right expression
     * @author Elif Kurtay
     * @since 1.3
     */
    @Experimental
    record Or(ExpressionDef left, ExpressionDef right) implements ConditionExpressionDef {
    }

    /**
     * The if-else expression.
     *
     * @param condition      The condition
     * @param expression     The expression if the condition is true
     * @param elseExpression The expression if the condition is false
     */
    @Experimental
    record IfElse(ExpressionDef condition, ExpressionDef expression,
                  ExpressionDef elseExpression) implements ExpressionDef {
        @Override
        public TypeDef type() {
            return expression.type();
        }
    }

    /**
     * The switch expression.
     * Note: null constant or null value represents a default case.
     *
     * @param expression  The switch expression
     * @param type        The switch type
     * @param cases       The cases
     * @param defaultCase The default case
     * @since 1.2
     */
    @Experimental
    record Switch(ExpressionDef expression,
                  TypeDef type,
                  Map<Constant, ? extends ExpressionDef> cases,
                  ExpressionDef defaultCase) implements ExpressionDef {
    }

    /**
     * The switch yield case expression.
     *
     * @param type      The yield result
     * @param statement The statement that should yield the result
     * @since 1.2
     */
    @Experimental
    record SwitchYieldCase(TypeDef type, StatementDef statement) implements ExpressionDef {
    }

    /**
     * The new array expression.
     *
     * @param type The type
     * @param size The array size
     * @author Denis Stepanov
     * @since 1.2
     */
    @Experimental
    record NewArrayOfSize(TypeDef.Array type, int size) implements ExpressionDef {
    }

    /**
     * The new array expression.
     *
     * @param type        The type
     * @param expressions The items expression
     * @author Denis Stepanov
     * @since 1.2
     */
    @Experimental
    record NewArrayInitialized(TypeDef.Array type,
                               List<? extends ExpressionDef> expressions) implements ExpressionDef {
    }

    /**
     * The get property value expression.
     *
     * @param instance        The instance
     * @param propertyElement The property element
     * @author Denis Stepanov
     * @since 1.3
     */
    @Experimental
    record GetPropertyValue(ExpressionDef instance,
                            PropertyElement propertyElement) implements ExpressionDef {

        @Override
        public TypeDef type() {
            return TypeDef.of(propertyElement.getType());
        }
    }

    /**
     * The get class expression.
     *
     * @param instance The instance
     * @author Denis Stepanov
     * @since 1.3
     */
    @Experimental
    record InvokeGetClassMethod(ExpressionDef instance) implements ExpressionDef {

        @Override
        public TypeDef type() {
            return TypeDef.of(Class.class);
        }
    }

    /**
     * The get hashCode expression.
     *
     * @param instance The instance
     * @author Denis Stepanov
     * @since 1.3
     */
    @Experimental
    record InvokeHashCodeMethod(ExpressionDef instance) implements ExpressionDef {

        @Override
        public TypeDef type() {
            return TypeDef.of(int.class);
        }
    }

    /**
     * The structurally equals expression.
     *
     * @param instance The instance
     * @param other    The other
     * @author Denis Stepanov
     * @since 1.3
     */
    @Experimental
    record EqualsStructurally(ExpressionDef instance,
                              ExpressionDef other) implements ConditionExpressionDef {
    }

    /**
     * The referential equals expression.
     *
     * @param instance The instance
     * @param other    The other
     * @author Denis Stepanov
     * @since 1.3
     */
    @Experimental
    record EqualsReferentially(ExpressionDef instance,
                               ExpressionDef other) implements ConditionExpressionDef {
    }

    /**
     * The instance of expression.
     *
     * @param expression   The expression
     * @param instanceType The instance type
     * @author Denis Stepanov
     * @since 1.5
     */
    @Experimental
    record InstanceOf(ExpressionDef expression,
                      ClassTypeDef instanceType) implements ConditionExpressionDef, ExpressionDef {
    }

    /**
     * The get array element expression.
     *
     * @param expression The expression
     * @param type       The component type
     * @param index      The index
     * @author Denis Stepanov
     * @since 1.5
     */
    @Experimental
    record ArrayElement(ExpressionDef expression,
                        TypeDef type,
                        int index) implements ExpressionDef {

        public ArrayElement(ExpressionDef expression,
                            int index) {
            this(expression, findComponentType(expression.type()), index);
        }

        private static TypeDef findComponentType(TypeDef arrayType) {
            if (arrayType instanceof TypeDef.Array array) {
                return array.componentType();
            }
            throw new IllegalArgumentException(arrayType + " is not an array");
        }

    }

    /**
     * The conditional expression.
     *
     * @author Denis Stepanov
     * @since 1.5
     */
    sealed interface ConditionExpressionDef extends ExpressionDef {

        @Override
        default TypeDef type() {
            return TypeDef.Primitive.BOOLEAN;
        }

    }

}
