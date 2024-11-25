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
package io.micronaut.sourcegen.bytecode.statement;

import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.bytecode.TypeUtils;
import io.micronaut.sourcegen.bytecode.expression.ExpressionWriter;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

final class AssignVariableStatementWriter implements StatementWriter {

    private final StatementDef.Assign assign;

    public AssignVariableStatementWriter(StatementDef.Assign assign) {
        this.assign = assign;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context, Runnable finallyBlock) {
        VariableDef.Local local = assign.variable();
        Type localType = TypeUtils.getType(local.type(), context.objectDef());
        ExpressionWriter.pushExpression(generatorAdapter, context, assign.expression(), local.type());
        Integer localIndex = context.locals().get(local.name());
        generatorAdapter.storeLocal(localIndex, localType);
    }
}
