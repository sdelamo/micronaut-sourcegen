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
package io.micronaut.sourcegen.generator.bytecode;

import io.micronaut.inject.ast.Element;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.bytecode.ByteCodeWriter;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.model.ObjectDef;

import java.io.OutputStream;
import java.io.Writer;

/**
 * Generates the classes directly by writing the bytecode.
 *
 * @author Denis Stepanov
 * @since 1.5
 */
public final class ByteCodeGenerator implements SourceGenerator {

    private static final ByteCodeWriter BYTE_CODE_WRITER = new ByteCodeWriter(false, true);

    @Override
    public VisitorContext.Language getLanguage() {
        return VisitorContext.Language.JAVA;
    }

    @Override
    public void write(ObjectDef objectDef, Writer writer) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void write(ObjectDef objectDef, VisitorContext context, Element... originatingElements) {
        try (OutputStream os = context.visitClass(objectDef.getName(), originatingElements)) {
            os.write(BYTE_CODE_WRITER.write(objectDef));
        } catch (Exception e) {
            Element element = originatingElements.length > 0 ? originatingElements[0] : null;
            throw new ProcessingException(element, "Failed to generate '" + objectDef.getName() + "': " + e.getMessage(), e);
        }
    }

}
