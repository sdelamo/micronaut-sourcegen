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

import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.bytecode.TypeUtils;
import io.micronaut.sourcegen.model.ExpressionDef;
import org.objectweb.asm.commons.GeneratorAdapter;

final class MathExpressionWriter implements ExpressionWriter {

    private final ExpressionDef.MathOp math;

    public MathExpressionWriter(ExpressionDef.MathOp math) {
        this.math = math;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context, boolean statement) {
        ExpressionWriter.pushExpression(generatorAdapter, context, math.left(), math.left().type());
        ExpressionWriter.pushExpression(generatorAdapter, context, math.right(), math.right().type());
        generatorAdapter.math(getMathOp(math.operator()), TypeUtils.getType(math.left().type(), context.objectDef()));
    }

    private static int getMathOp(String op) {
        return switch (op.trim()) {
            case "+" -> GeneratorAdapter.ADD;
            case "*" -> GeneratorAdapter.MUL;
            case "|" -> GeneratorAdapter.OR;
            default -> throw new UnsupportedOperationException("Unrecognized math operator: " + op);
        };
    }
}
