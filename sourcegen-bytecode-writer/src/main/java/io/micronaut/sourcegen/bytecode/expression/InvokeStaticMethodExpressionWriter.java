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
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Iterator;

import static io.micronaut.sourcegen.bytecode.WriterUtils.asMethod;
import static io.micronaut.sourcegen.bytecode.WriterUtils.popValue;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

final class InvokeStaticMethodExpressionWriter implements ExpressionWriter {

    private final ExpressionDef.InvokeStaticMethod invokeStaticMethod;

    public InvokeStaticMethodExpressionWriter(ExpressionDef.InvokeStaticMethod invokeStaticMethod) {
        this.invokeStaticMethod = invokeStaticMethod;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context, boolean statement) {
        Iterator<ParameterDef> iterator = invokeStaticMethod.method().getParameters().iterator();
        for (ExpressionDef value : invokeStaticMethod.values()) {
            ExpressionWriter.pushExpression(generatorAdapter, context, value, iterator.next().getType());
        }
        boolean isDeclaringTypeInterface = invokeStaticMethod.classDef().isInterface();
        MethodDef methodDef = invokeStaticMethod.method();
        Method method = asMethod(context, methodDef);
        Type type = TypeUtils.getType(invokeStaticMethod.classDef(), context.objectDef());

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
}
