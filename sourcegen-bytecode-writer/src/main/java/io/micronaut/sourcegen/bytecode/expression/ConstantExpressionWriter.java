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
import io.micronaut.sourcegen.model.TypeDef;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Array;
import java.util.Arrays;

final class ConstantExpressionWriter implements ExpressionWriter {
    private final ExpressionDef.Constant constant;

    public ConstantExpressionWriter(ExpressionDef.Constant constant) {
        this.constant = constant;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context) {
        TypeDef type = constant.type();
        Object value = constant.value();
        if (value == null) {
            generatorAdapter.push((String) null);
            return;
        }
        if (value.getClass().isArray()) {
            ExpressionDef ex = TypeDef.of(value.getClass().getComponentType())
                .array()
                .instantiate(Arrays.stream(getArray(value)).map(ExpressionDef::constant).toList());
            ExpressionWriter.writeExpression(generatorAdapter, context, ex);
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
            generatorAdapter.push(TypeUtils.getType(typeDef, context.objectDef()));
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

    private static Object[] getArray(Object val) {
        if (val instanceof Object[]) {
            return (Object[]) val;
        }
        Object[] outputArray = new Object[Array.getLength(val)];
        for (int i = 0; i < outputArray.length; ++i) {
            outputArray[i] = Array.get(val, i);
        }
        return outputArray;
    }
}
