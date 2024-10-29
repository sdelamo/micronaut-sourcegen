package io.micronaut.sourcegen.javapoet.write;

import io.micronaut.sourcegen.JavaPoetSourceGenerator;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.PropertyDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EnumWriteTest {
    @Test
    public void writeSimpleEnum() throws IOException {
        EnumDef enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE")
            .addEnumConstant("IN_PROGRESS")
            .addEnumConstant("DELETED")
            .build();
        var result = writeEnum(enumDef);

        var expected = """
        package test;

        enum Status {

          ACTIVE,
          IN_PROGRESS,
          DELETED
        }
        """;
        assertEquals(expected.strip(), result.strip());
    }

    @Test
    public void writeComplexEnumConstant() throws IOException {
        EnumDef enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("active")
            .addEnumConstant("in-progress")
            .addEnumConstant("deleted")
            .build();
        var result = writeEnum(enumDef);

        var expected = """
        package test;

        enum Status {

          ACTIVE("active"),
          IN_PROGRESS("in-progress"),
          DELETED("deleted")
        }
        """;
        assertEquals(expected.strip(), result.strip());
    }

    @Test
    public void writeComplexEnumConstant2() throws IOException {
        EnumDef enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE", ExpressionDef.constant(2))
            .addEnumConstant("IN_PROGRESS", ExpressionDef.constant(1))
            .addEnumConstant("DELETED", ExpressionDef.constant(0))
            .build();
        var result = writeEnum(enumDef);

        var expected = """
        package test;

        enum Status {

          ACTIVE(2),
          IN_PROGRESS(1),
          DELETED(0)
        }
        """;
        assertEquals(expected.strip(), result.strip());
    }

    @Test
    public void writeComplexEnumWithProperty() throws IOException {
        EnumDef enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("active")
            .addEnumConstant("in-progress")
            .addEnumConstant("deleted")
            .addProperty(PropertyDef.builder("value").ofType(TypeDef.STRING).build())
            .build();
        var result = writeEnum(enumDef);

        var expected = """
        package test;

        import java.lang.String;

        enum Status {

          ACTIVE("active"),
          IN_PROGRESS("in-progress"),
          DELETED("deleted");

          private final String value;

          public Status(String value) {
            this.value = value;
          }
        }
        """;
        assertEquals(expected.strip(), result.strip());
    }


    private String writeEnum(EnumDef enumDef) throws IOException {
        JavaPoetSourceGenerator generator = new JavaPoetSourceGenerator();
        String result;
        try (StringWriter writer = new StringWriter()) {
            generator.write(enumDef, writer);
            result = writer.toString();
        }

        // The regex will skip the imports and make sure it is a record
        final Pattern ENUM_REGEX = Pattern.compile("package [^;]+;[^/]+" +
            "enum " + enumDef.getSimpleName() + " \\{\\s+" +
            "([\\s\\S]+)\\s+}\\s+");
        Matcher matcher = ENUM_REGEX.matcher(result);
        if (!matcher.matches()) {
            fail("Expected enum to match regex: \n" + ENUM_REGEX + "\nbut is: \n" + result);
        }
        return matcher.group(0);
    }

}
