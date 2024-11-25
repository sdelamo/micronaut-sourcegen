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
package io.micronaut.sourcegen.bytecode.expression;

import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * The expression writer.
 *
 * @author Denis Stepano
 * @since 1.5
 */
public sealed interface ExpressionWriter permits ArrayElementExpressionWriter, CastExpressionWriter, ConditionExpressionWriter, ConstantExpressionWriter, GetPropertyExpressionWriter, IfElseExpressionWriter, InstanceOfExpressionWriter, InvokeGetClassExpressionWriter, InvokeHashCodeMethodExpressionWriter, InvokeInstanceMethodExpressionWriter, InvokeStaticMethodExpressionWriter, MathExpressionWriter, NewArrayInitializedExpressionWriter, NewArrayOfSizeExpressionWriter, NewInstanceExpressionWriter, SwitchExpressionWriter, SwitchYieldCaseExpressionWriter, VariableExpressionWriter {

    /**
     * Create a writer from an expression.
     *
     * @param expressionDef The expression
     * @return the writer
     */
    static ExpressionWriter of(ExpressionDef expressionDef) {
        if (expressionDef instanceof ExpressionDef.ArrayElement arrayElement) {
            return new ArrayElementExpressionWriter(arrayElement);
        }
        if (expressionDef instanceof ExpressionDef.InstanceOf instanceOf) {
            return new InstanceOfExpressionWriter(instanceOf);
        }
        if (expressionDef instanceof ExpressionDef.ConditionExpressionDef) {
            return new ConditionExpressionWriter(expressionDef);
        }
        if (expressionDef instanceof ExpressionDef.MathOp math) {
            return new MathExpressionWriter(math);
        }
        if (expressionDef instanceof ExpressionDef.InvokeInstanceMethod invokeInstanceMethod) {
            return new InvokeInstanceMethodExpressionWriter(invokeInstanceMethod);
        }
        if (expressionDef instanceof ExpressionDef.NewInstance newInstance) {
            return new NewInstanceExpressionWriter(newInstance);
        }
        if (expressionDef instanceof ExpressionDef.NewArrayOfSize newArray) {
            return new NewArrayOfSizeExpressionWriter(newArray);
        }
        if (expressionDef instanceof ExpressionDef.NewArrayInitialized newArray) {
            return new NewArrayInitializedExpressionWriter(newArray);
        }
        if (expressionDef instanceof ExpressionDef.Cast castExpressionDef) {
            return new CastExpressionWriter(castExpressionDef);
        }
        if (expressionDef instanceof ExpressionDef.Constant constant) {
            return new ConstantExpressionWriter(constant);
        }
        if (expressionDef instanceof ExpressionDef.InvokeStaticMethod invokeStaticMethod) {
            return new InvokeStaticMethodExpressionWriter(invokeStaticMethod);
        }
        if (expressionDef instanceof ExpressionDef.GetPropertyValue getPropertyValue) {
            return new GetPropertyExpressionWriter(getPropertyValue);
        }
        if (expressionDef instanceof ExpressionDef.IfElse conditionIfElse) {
            return new IfElseExpressionWriter(conditionIfElse);
        }
        if (expressionDef instanceof ExpressionDef.Switch aSwitch) {
            return new SwitchExpressionWriter(aSwitch);
        }
        if (expressionDef instanceof ExpressionDef.SwitchYieldCase switchYieldCase) {
            return new SwitchYieldCaseExpressionWriter(switchYieldCase);
        }
        if (expressionDef instanceof VariableDef variableDef) {
            return new VariableExpressionWriter(variableDef);
        }
        if (expressionDef instanceof ExpressionDef.InvokeGetClassMethod invokeGetClassMethod) {
            return new InvokeGetClassExpressionWriter(invokeGetClassMethod);
        }
        if (expressionDef instanceof ExpressionDef.InvokeHashCodeMethod invokeHashCodeMethod) {
            return new InvokeHashCodeMethodExpressionWriter(invokeHashCodeMethod);
        }
        throw new UnsupportedOperationException("Unrecognized expression: " + expressionDef);
    }

    static void pushExpression(GeneratorAdapter generatorAdapter,
                               MethodContext context,
                               ExpressionDef expressionDef,
                               TypeDef expectedType) {
        pushExpression(generatorAdapter, context, expressionDef, expectedType, false);
    }

    static void pushExpression(GeneratorAdapter generatorAdapter,
                               MethodContext context,
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
        ExpressionWriter.of(expressionDef).write(generatorAdapter, context, statement);
        TypeDef type = expressionDef.type();
        CastExpressionWriter.cast(generatorAdapter, context, type, expectedType);
    }

    /**
     * Write the expression.
     *
     * @param generatorAdapter The adapter
     * @param context          The method context
     * @param statement        If represented as a statement
     */
    void write(GeneratorAdapter generatorAdapter,
               MethodContext context,
               boolean statement);

}
