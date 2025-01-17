package io.micronaut.sourcegen.example.plugin.gradle;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPluginTest extends AbstractPluginTest {

    @Test
    void generateAndBuildSimpleRecord() {
        settingsFile("rootProject.name = 'test-project'");
        buildFile("""
        plugins {
            id "io.micronaut.sourcegen.test"
            id "java"
        }

        test {
            generateRecordWithName("MyRecord", "io.micronaut.test", spec -> {
                spec.getJavadoc().add("A simple record")
                spec.getProperties().put("title", "java.lang.String")
                spec.getProperties().put("age", "java.lang.Integer")
            })
            generateResource("generateHello", "META-INF/hello.txt", "Hello!");
        }

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
        }
        """);

        var result = configureRunner(":build").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMyRecord").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava").getOutcome());

        File generated = file("build/generated/generateMyRecord/src/main/java/io/micronaut/test/MyRecord.java");
        assertTrue(generated.exists());
        assertEquals(content(generated), """
            package io.micronaut.test;

            /**
             * Version: 1
             * A simple record
             */
            public record MyRecord(
                java.lang.String title,
                java.lang.Integer age
            ) {
            }
            """);

        assertTrue(file("build/classes/java/main/io/micronaut/test/MyRecord.class").exists());

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHello").getOutcome());
        File generatedResource = file("build/generated/generateHello/META-INF/hello.txt");
        assertTrue(generatedResource.exists());
        assertEquals("Hello!", content(generatedResource));
    }

    @Test
    void generateSimpleResource() {
        settingsFile("rootProject.name = 'test-project'");
        buildFile("""
        plugins {
            id "io.micronaut.sourcegen.test"
            id "java"
        }

        test {
            generateResource("generateHello", "META-INF/hello.txt", "Hello!");
        }

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
        }
        """);

        var result = configureRunner(":generateHello").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHello").getOutcome());

        File generatedResource = file("build/generated/generateHello/META-INF/hello.txt");
        assertTrue(generatedResource.exists());
        assertEquals("Hello!", content(generatedResource));
    }

    @Test
    void failOnRequiredProperty() {
        settingsFile("rootProject.name = 'test-project'");
        buildFile("""
        plugins {
            id "io.micronaut.sourcegen.test"
            id "java"
        }

        test {
            generateSimpleRecord("generate1", spec -> {})
        }

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
        }
        """);

        var result = configureRunner(":build").buildAndFail();
        assertTrue(result.getOutput().contains("property 'typeName' doesn't have a configured value."));
    }

}
