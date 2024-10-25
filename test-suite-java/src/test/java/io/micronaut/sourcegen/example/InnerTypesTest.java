package io.micronaut.sourcegen.example;

import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.PropertyDef;
import io.micronaut.sourcegen.model.RecordDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InnerTypesTest {

    @Test
    public void enumInClass() throws IOException {
        String expectedString = "class StatusClass {\n" +
            "  enum Status {\n" +
            "\n" +
            "    SINGLE,\n" +
            "    MARRIED\n" +
            "  }\n" +
            "}\n";
        EnumDef.EnumDefBuilder enumBuilder = EnumDef.builder("Status");
        enumBuilder.addEnumConstant("SINGLE").addEnumConstant("MARRIED");
        EnumDef enumDef = enumBuilder.build();

        ClassDef.ClassDefBuilder classBuilder = getClassDefBuilderWith(enumDef);
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void recordInClass() throws IOException {
        String expectedString = "class ExampleRecordClass {\n" +
            "  record ExampleRecord(\n" +
            "      int id\n" +
            "  ) {\n" +
            "  }\n" +
            "}\n";
        RecordDef.RecordDefBuilder recordBuilder = RecordDef.builder("ExampleRecord");
        PropertyDef.PropertyDefBuilder propertyBuilder = PropertyDef.builder("id").ofType(TypeDef.Primitive.INT);
        recordBuilder.addProperty(propertyBuilder.build());

        ClassDef.ClassDefBuilder classBuilder = getClassDefBuilderWith(recordBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void classInClass() throws IOException {
        String expectedString = "class InnerClass {\n" +
            "  class Inner {\n" +
            "  }\n" +
            "}\n";
        ClassDef.ClassDefBuilder innerClassBuilder = ClassDef.builder("Inner");

        ClassDef.ClassDefBuilder classBuilder = getClassDefBuilderWith(innerClassBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void interfaceInClass() throws IOException {
        String expectedString = "class InterfaceClass {\n" +
            "  interface Interface {\n" +
            "  }\n" +
            "}\n";
        InterfaceDef.InterfaceDefBuilder interfaceBuilder = InterfaceDef.builder("Interface");

        ClassDef.ClassDefBuilder classBuilder = getClassDefBuilderWith(interfaceBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void enumInRecord() throws IOException {
        String expectedString = "record StatusRecord() {\n" +
            "  enum Status {\n" +
            "\n" +
            "    SINGLE,\n" +
            "    MARRIED\n" +
            "  }\n" +
            "}\n";
        EnumDef.EnumDefBuilder enumBuilder = EnumDef.builder("Status");
        enumBuilder.addEnumConstant("SINGLE").addEnumConstant("MARRIED");
        EnumDef enumDef = enumBuilder.build();

        RecordDef.RecordDefBuilder classBuilder = getRecordDefBuilderWith(enumDef);
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void recordInRecord() throws IOException {
        String expectedString = "record ExampleRecord() {\n" +
            "  record Example(\n" +
            "      int id\n" +
            "  ) {\n" +
            "  }\n" +
            "}\n";
        RecordDef.RecordDefBuilder recordBuilder = RecordDef.builder("Example");
        PropertyDef.PropertyDefBuilder propertyBuilder = PropertyDef.builder("id").ofType(TypeDef.Primitive.INT);
        recordBuilder.addProperty(propertyBuilder.build());

        RecordDef.RecordDefBuilder classBuilder = getRecordDefBuilderWith(recordBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void classInRecord() throws IOException {
        String expectedString = "record InnerRecord() {\n" +
            "  class Inner {\n" +
            "  }\n" +
            "}\n";
        ClassDef.ClassDefBuilder innerClassBuilder = ClassDef.builder("Inner");

        RecordDef.RecordDefBuilder classBuilder = getRecordDefBuilderWith(innerClassBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void interfaceInRecord() throws IOException {
        String expectedString = "record InterfaceRecord() {\n" +
            "  interface Interface {\n" +
            "  }\n" +
            "}\n";
        InterfaceDef.InterfaceDefBuilder interfaceBuilder = InterfaceDef.builder("Interface");

        RecordDef.RecordDefBuilder classBuilder = getRecordDefBuilderWith(interfaceBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void enumInInterface() throws IOException {
        String expectedString = "interface StatusInterface {\n" +
            "  enum Status {\n" +
            "\n" +
            "    SINGLE,\n" +
            "    MARRIED\n" +
            "  }\n" +
            "}\n";
        EnumDef.EnumDefBuilder enumBuilder = EnumDef.builder("Status");
        enumBuilder.addEnumConstant("SINGLE").addEnumConstant("MARRIED");
        EnumDef enumDef = enumBuilder.build();

        InterfaceDef.InterfaceDefBuilder classBuilder = getInterfaceDefBuilderWith(enumDef);
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void recordInInterface() throws IOException {
        String expectedString = "interface RecordInterface {\n" +
            "  record Record(\n" +
            "      int id\n" +
            "  ) {\n" +
            "  }\n" +
            "}\n";
        RecordDef.RecordDefBuilder recordBuilder = RecordDef.builder("Record");
        PropertyDef.PropertyDefBuilder propertyBuilder = PropertyDef.builder("id").ofType(TypeDef.Primitive.INT);
        recordBuilder.addProperty(propertyBuilder.build());

        InterfaceDef.InterfaceDefBuilder classBuilder = getInterfaceDefBuilderWith(recordBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void classInInterface() throws IOException {
        String expectedString = "interface InnerInterface {\n" +
            "  class Inner {\n" +
            "  }\n" +
            "}\n";
        ClassDef.ClassDefBuilder innerClassBuilder = ClassDef.builder("Inner");

        InterfaceDef.InterfaceDefBuilder classBuilder = getInterfaceDefBuilderWith(innerClassBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    @Test
    public void interfaceInInterface() throws IOException {
        String expectedString = "interface InnerInterface {\n" +
            "  interface Inner {\n" +
            "  }\n" +
            "}\n";
        InterfaceDef.InterfaceDefBuilder interfaceBuilder = InterfaceDef.builder("Inner");

        InterfaceDef.InterfaceDefBuilder classBuilder = getInterfaceDefBuilderWith(interfaceBuilder.build());
        File outputFile = generateClass(classBuilder.build());
        assertTrue(outputFile.exists());
        String actual = compareFileContentWithString(outputFile, expectedString);
        assertEquals(expectedString, actual);
        outputFile.delete();
    }

    private static ClassDef.ClassDefBuilder getClassDefBuilderWith(ObjectDef objectDef) {
        ClassDef.ClassDefBuilder classBuilder = ClassDef.builder(objectDef.getSimpleName() + "Class");
        classBuilder.addInnerType(objectDef);
        return classBuilder;
    }

    private static RecordDef.RecordDefBuilder getRecordDefBuilderWith(ObjectDef objectDef) {
        RecordDef.RecordDefBuilder classBuilder = RecordDef.builder(objectDef.getSimpleName() + "Record");
        classBuilder.addInnerType(objectDef);
        return classBuilder;
    }

    private static InterfaceDef.InterfaceDefBuilder getInterfaceDefBuilderWith(ObjectDef objectDef) {
        InterfaceDef.InterfaceDefBuilder classBuilder = InterfaceDef.builder(objectDef.getSimpleName() + "Interface");
        classBuilder.addInnerType(objectDef);
        return classBuilder;
    }

    private static File generateClass(ObjectDef objectDef) throws IOException {
        try {
            SourceGenerator sourceGenerator = SourceGenerators
                .findByLanguage(VisitorContext.Language.JAVA).orElse(null);
            if (sourceGenerator == null) {
                return null;
            }

            File outputFile = new File("Example.java");

            if (!outputFile.exists() && !outputFile.createNewFile()) {
                throw new IOException("Could not create file " + outputFile.getAbsolutePath());
            }
            try (FileWriter writer = new FileWriter(outputFile)) {
                sourceGenerator.write(objectDef, writer);
            }
            return outputFile;
        } catch (ProcessingException | IOException e) {
            throw e;
        }
    }

    private static String compareFileContentWithString(File file, String comparisonString) {
        try {
            return new String(Files.readAllBytes(Path.of(file.toURI())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
