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

final class MathBinaryExpressionWriter implements ExpressionWriter {

    private final ExpressionDef.MathBinaryOperation math;

    public MathBinaryExpressionWriter(ExpressionDef.MathBinaryOperation math) {
        this.math = math;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context) {
        ExpressionWriter.writeExpression(generatorAdapter, context, math.left());
        ExpressionWriter.writeExpression(generatorAdapter, context, math.right());
        generatorAdapter.math(getMathOp(math.opType()), TypeUtils.getType(math.left().type(), context.objectDef()));
    }

    private static int getMathOp(ExpressionDef.MathBinaryOperation.OpType opType) {
        return switch (opType) {
            case ADDITION -> GeneratorAdapter.ADD;
            case SUBTRACTION -> GeneratorAdapter.SUB;
            case MULTIPLICATION -> GeneratorAdapter.MUL;
            case DIVISION -> GeneratorAdapter.DIV;
            case MODULUS -> GeneratorAdapter.REM;

            case BITWISE_AND -> GeneratorAdapter.AND;
            case BITWISE_OR -> GeneratorAdapter.OR;
            case BITWISE_XOR -> GeneratorAdapter.XOR;
            case BITWISE_LEFT_SHIFT -> GeneratorAdapter.SHL;
            case BITWISE_RIGHT_SHIFT -> GeneratorAdapter.SHR;
            case BITWISE_UNSIGNED_RIGHT_SHIFT -> GeneratorAdapter.USHR;
        };
    }
}
