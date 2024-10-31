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
package io.micronaut.sourcegen.custom.visitor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.Builder;
import io.micronaut.sourcegen.custom.example.GenerateInnerTypes;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.PropertyDef;
import io.micronaut.sourcegen.model.RecordDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;

import javax.lang.model.element.Modifier;
import java.util.List;

import static io.micronaut.sourcegen.custom.visitor.GenerateInnerTypeInEnumVisitor.getInnerClassDef;
import static io.micronaut.sourcegen.custom.visitor.GenerateInnerTypeInEnumVisitor.getInnerEnumDef;
import static io.micronaut.sourcegen.custom.visitor.GenerateInnerTypeInEnumVisitor.getInnerInterfaceDef;
import static io.micronaut.sourcegen.custom.visitor.GenerateInnerTypeInEnumVisitor.getInnerRecordDef;

@Internal
public final class GenerateInnerTypeInRecordVisitor implements TypeElementVisitor<GenerateInnerTypes, Object> {

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
        if (sourceGenerator == null) {
            return;
        }

        EnumDef enumDef = getEnumDef(element);

        context.visitGeneratedSourceFile(enumDef.getPackageName(), enumDef.getSimpleName(), element)
            .ifPresent(generatedFile -> {
                try {
                    generatedFile.write(writer -> sourceGenerator.write(enumDef, writer));
                } catch (Exception e) {
                    throw new ProcessingException(element, e.getMessage(), e);
                }
            });
    }

    private static EnumDef getEnumDef(ClassElement element) {
        String enumClassName = element.getPackageName() + ".RecordWithInnerTypes";

        EnumDef innerEnum = getInnerEnumDef();

        RecordDef innerRecord = getInnerRecordDef();

        ClassDef innerClass = getInnerClassDef();

        InterfaceDef innerInterface = getInnerInterfaceDef();

        //outer enum
        return EnumDef.builder(enumClassName)
            .addModifiers(Modifier.PUBLIC)
            .addEnumConstant("A")
            .addEnumConstant("B")
            .addEnumConstant("C")
            .addInnerType(innerEnum)
            .addInnerType(innerRecord)
            .addInnerType(innerClass)
            .addInnerType(innerInterface)
            .addMethod(MethodDef.builder("myName")
                .addModifiers(Modifier.PUBLIC)
                .addStatement(new StatementDef.Return(
                    ExpressionDef.invoke(
                        new VariableDef.This(ClassTypeDef.of(enumClassName)),
                        "toString",
                        List.of(),
                        TypeDef.of(String.class)
                    )
                ))
                .returns(String.class)
                .build())
            .build();
    }
}
