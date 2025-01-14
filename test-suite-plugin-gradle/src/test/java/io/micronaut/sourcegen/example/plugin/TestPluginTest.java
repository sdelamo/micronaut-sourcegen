package io.micronaut.sourcegen.example.plugin;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;

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
            generateSimpleRecord("generate1", spec -> {
                spec.getTypeName().set("MyRecord")
                spec.getJavadoc().add("A simple record")
                spec.getProperties().put("title", "java.lang.String")
                spec.getProperties().put("age", "java.lang.Integer")
            })
        }

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
        }
        """);

        var result = configureRunner(":build").build();

        System.out.println("Tasks: " + result.getTasks().stream().map(BuildTask::getPath).toList());
        assertEquals(TaskOutcome.SUCCESS, result.task(":generate1").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava").getOutcome());

        File generated = file("build/generated/generate1/src/main/java/com/example/MyRecord.java");
        assertTrue(generated.exists());
        assertEquals(content(generated), """
            package com.example;

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

        assertTrue(file("build/classes/java/main/com/example/MyRecord.class").exists());
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
