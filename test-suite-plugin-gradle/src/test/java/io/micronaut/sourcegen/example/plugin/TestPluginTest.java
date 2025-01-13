package io.micronaut.sourcegen.example.plugin;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPluginTest extends AbstractPluginTest {

    @Test
    void canRunPlugin() {
        settingsFile("rootProject.name = 'test-project'");
        buildFile("""
        plugins {
            id "io.micronaut.sourcegen.test"
        }

        Test {
            run("run1", spec -> {
                spec.getHeader().set("hello")
            })
        }

        repositories {
            mavenLocal()
            mavenCentral()
        }

        dependencies {
        }
        """);

        var result = configureRunner(":run1").build();

        System.out.println("Tasks: " + result.getTasks().stream().map(BuildTask::getPath).toList());
        assertEquals(TaskOutcome.SUCCESS, result.task(":run1").getOutcome());
    }

}
