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

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.bytecode.TypeUtils;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.objectweb.asm.commons.GeneratorAdapter;

final class CastExpressionWriter implements ExpressionWriter {
    private final ExpressionDef.Cast castExpressionDef;

    public CastExpressionWriter(ExpressionDef.Cast castExpressionDef) {
        this.castExpressionDef = castExpressionDef;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context, boolean statement) {
        ExpressionDef exp = castExpressionDef.expressionDef();
        TypeDef from = exp.type();
        ExpressionWriter.pushExpression(generatorAdapter, context, exp, from);
        TypeDef to = castExpressionDef.type();
        cast(generatorAdapter, context, from, to);
    }

    static void cast(GeneratorAdapter generatorAdapter, MethodContext context, TypeDef from, TypeDef to) {
        from = ObjectDef.getContextualType(context.objectDef(), from);
        to = ObjectDef.getContextualType(context.objectDef(), to);
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

    private static void checkCast(GeneratorAdapter generatorAdapter, MethodContext context, TypeDef from, TypeDef to) {
        TypeDef toType = ObjectDef.getContextualType(context.objectDef(), to);
        if (!toType.makeNullable().equals(from.makeNullable())) {
            generatorAdapter.checkCast(TypeUtils.getType(toType, context.objectDef()));
        }
    }

    private static void unbox(GeneratorAdapter generatorAdapter, MethodContext context, TypeDef to) {
        generatorAdapter.unbox(TypeUtils.getType(to, context.objectDef()));
    }

    private static void box(GeneratorAdapter generatorAdapter, MethodContext context, TypeDef from) {
        generatorAdapter.valueOf(TypeUtils.getType(from, context.objectDef()));
    }
}
