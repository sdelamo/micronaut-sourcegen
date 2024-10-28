package io.micronaut.sourcegen.javapoet.write;

import io.micronaut.sourcegen.JavaPoetSourceGenerator;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.PropertyDef;
import io.micronaut.sourcegen.model.RecordDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

public class FieldDefTest extends AbstractWriteTest {

    @Test
    public void arrayField() throws IOException {
        TypeDef array = TypeDef.array(TypeDef.primitive("byte"));
        String result = writeClassWithField(
            FieldDef.builder("byteArray").ofType(array).build()
        );

        assertEquals("byte[] byteArray;", result);
    }

    @Test
    public void twoDimensionalArrayField() throws IOException {
        TypeDef array = TypeDef.array(
            TypeDef.primitive("byte"), 2
        );
        String result = writeClassWithField(
            FieldDef.builder("byteArray").ofType(array).build()
        );

        assertEquals("byte[][] byteArray;", result);
    }


    @Test public void annotatedGenericField() throws Exception {
        PropertyDef.PropertyDefBuilder propertyDef = PropertyDef.builder("numbers");
        var MIN_ANN = AnnotationDef.builder(ClassTypeDef.of("jakarta.validation.constraints.Min")).addMember("value", 1).build();
        propertyDef.ofType(TypeDef.parameterized(ClassTypeDef.of(List.class), TypeDef.Primitive.FLOAT.wrapperType().annotated(MIN_ANN)));

        RecordDef.RecordDefBuilder builder = RecordDef.builder("Record").addProperty(propertyDef.build());
        JavaPoetSourceGenerator generator = new JavaPoetSourceGenerator();
        String result;
        try (StringWriter writer = new StringWriter()) {
            generator.write(builder.build(), writer);
            result = writer.toString();
        }

        assertThat(result).isEqualTo("import jakarta.validation.constraints.Min;\n" +
            "import java.lang.Float;\n" +
            "import java.util.List;\n" +
            "\n" +
            "record Record(\n" +
            "    List<@Min(1) Float> numbers\n" +
            ") {\n" +
            "}\n");
    }

}
