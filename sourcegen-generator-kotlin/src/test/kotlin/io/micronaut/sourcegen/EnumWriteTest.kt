package io.micronaut.sourcegen

import io.micronaut.sourcegen.model.*
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.StringWriter
import java.util.regex.Pattern
import javax.lang.model.element.Modifier

class EnumWriteTest {
    @Test
    @Throws(IOException::class)
    fun writeSimpleEnum() {
        val enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE")
            .addEnumConstant("IN_PROGRESS")
            .addEnumConstant("DELETED")
            .build()
        val result = writeEnum(enumDef)

        val expected = """
        package test

        public enum class Status {
          ACTIVE,
          IN_PROGRESS,
          DELETED,
        }

        """.trimIndent()
        Assert.assertEquals(expected.trim(), result.trim())
    }

    @Test
    fun testExceptions() {
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { EnumDef.builder("test.Status").addEnumConstant("active").build() }
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { EnumDef.builder("test.Status").addEnumConstant("9in progress", ExpressionDef.constant(1)).build() }
    }

    @Test
    @Throws(IOException::class)
    fun writeComplexEnumConstant() {
        val enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE", ExpressionDef.constant(2))
            .addEnumConstant("IN_PROGRESS", ExpressionDef.constant(1))
            .addEnumConstant("DELETED", ExpressionDef.constant(0))
            .build()
        val result = writeEnum(enumDef)

        val expected = """
        package test

        public enum class Status {
          ACTIVE(2),
          IN_PROGRESS(1),
          DELETED(0),
        }

        """.trimIndent()
        Assert.assertEquals(expected.trim(), result.trim())
    }

    @Test
    @Throws(IOException::class)
    fun writeComplexEnumWithProperty() {
        val enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE")
            .addEnumConstant("IN_PROGRESS")
            .addEnumConstant("DELETED")
            .addProperty(PropertyDef.builder("strValue").ofType(TypeDef.STRING).build())
            .build()
        val generator = KotlinPoetSourceGenerator()
        var result: String
        StringWriter().use { writer ->
            generator.write(enumDef, writer)
            result = writer.toString()
        }

        val expected = """
        package test

        import kotlin.String

        public enum class Status public constructor(
          public var strValue: String,
        ) {
          ACTIVE,
          IN_PROGRESS,
          DELETED,
          ;
        }
        """.trimIndent()
        Assert.assertEquals(expected.trim(), result.trim())
    }

    @Test
    @Throws(IOException::class)
    fun writeComplexEnumWithFieldMethod() {
        val enumDef = EnumDef.builder("test.Status")
            .addEnumConstant("ACTIVE")
            .addEnumConstant("IN_PROGRESS")
            .addEnumConstant("DELETED")
            .addField(FieldDef.builder("strValue").ofType(TypeDef.STRING).build())
            .addMethod(
                MethodDef.builder("getValue")
                    .returns(TypeDef.STRING)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement(StatementDef.Return(ExpressionDef.constant("value")))
                    .build()
            )
            .build()
        val result = writeEnum(enumDef)

        val expected = """
        package test

        import kotlin.String

        public enum class Status {
          ACTIVE,
          IN_PROGRESS,
          DELETED,
          ;

          public var strValue: String

          public fun getValue(): String {
            return "value"
          }
        }
        """.trimIndent()
        Assert.assertEquals(expected.trim(), result.trim())
    }


    @Throws(IOException::class)
    private fun writeEnum(enumDef: EnumDef): String {
        val generator: KotlinPoetSourceGenerator = KotlinPoetSourceGenerator()
        var result: String
        StringWriter().use { writer ->
            generator.write(enumDef, writer)
            result = writer.toString()
        }
        // The regex will skip the imports and make sure it is a record
        val ENUM_REGEX = Pattern.compile(
            "package test([\\s\\S]+)\\s+" +
                    "public enum class " + enumDef.simpleName + " \\{\\s+" +
                    "([\\s\\S]+)\\s+}\\s+"
        )
        val matcher = ENUM_REGEX.matcher(result)
        if (!matcher.matches()) {
            Assert.fail("Expected enum to match regex: \n$ENUM_REGEX\nbut is: \n$result")
        }
        return matcher.group(0)
    }
}
