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
import io.micronaut.sourcegen.bytecode.expression.ExpressionWriter;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Collection;
import java.util.Objects;

/**
 * The writer utils.
 *
 * @author Denis Stepanov
 * @since 1.5
 */
public final class WriterUtils {

    public static Method asMethod(MethodContext context, MethodDef methodDef) {
        return new Method(methodDef.getName(), getMethodDescriptor(context.objectDef(), methodDef));
    }

    public static String getConstructorDescriptor(@Nullable ObjectDef objectDef, Collection<TypeDef> types) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');

        for (TypeDef argumentType : types) {
            builder.append(TypeUtils.getType(argumentType, objectDef).getDescriptor());
        }

        return builder.append(")V").toString();
    }

    static String getMethodDescriptor(@Nullable ObjectDef objectDef, MethodDef methodDef) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        for (ParameterDef parameterDef : methodDef.getParameters()) {
            builder.append(TypeUtils.getType(parameterDef.getType(), objectDef));
        }
        builder.append(')');
        builder.append(TypeUtils.getType(Objects.requireNonNullElse(methodDef.getReturnType(), TypeDef.VOID), objectDef));
        return builder.toString();
    }

    public static void popValue(GeneratorAdapter generatorAdapter, TypeDef typeDef) {
        if (typeDef.equals(TypeDef.Primitive.LONG) || typeDef.equals(TypeDef.Primitive.DOUBLE)) {
            generatorAdapter.pop2();
        } else {
            generatorAdapter.pop();
        }
    }

    public static void pushSwitchExpression(GeneratorAdapter generatorAdapter,
                                            MethodContext context,
                                            ExpressionDef expression) {
        TypeDef switchExpressionType = expression.type();
        ExpressionWriter.pushExpression(generatorAdapter, context, expression, switchExpressionType);
        if (!switchExpressionType.equals(TypeDef.Primitive.INT)) {
            throw new UnsupportedOperationException("Not allowed switch expression type: " + switchExpressionType);
        }
    }

    public static int toSwitchKey(ExpressionDef.Constant constant) {
        if (constant.value() instanceof String s) {
            return s.hashCode();
        }
        if (constant.value() instanceof Integer i) {
            return i;
        }
        throw new UnsupportedOperationException("Unrecognized constant for a switch key: " + constant);
    }

}
