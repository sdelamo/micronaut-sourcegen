package io.micronaut.sourcegen.example.plugin.maven;

import org.junit.jupiter.api.Test;

import java.io.File;

public class TestMavenPluginTest extends AbstractMavenPluginTest {

    @Test
    void generateAndBuildSimpleRecord() throws Exception {
        File pom = new File("src/test/resources/test-pom.xml");

        GenerateSimpleRecordMojo mojo = (GenerateSimpleRecordMojo) findConfiguredMojo("generateSimpleRecord", pom);
        mojo.execute();

        File generated = file("src/main/java/io/micronaut/test/MyRecord.java");
        assertTrue(generated.exists());
        assertEquals(content(generated), """
            package io.micronaut.test;

            /**
             * Version: 1
             * A simple record
             */
            public record MyRecord(
                java.lang.Integer age,
                java.lang.String title
            ) {
            }
            """);
    }

    @Test
    void generateSimpleResource() throws Exception {
        File pom = new File("src/test/resources/test-resource-pom.xml");

        GenerateSimpleResourceMojo mojo = (GenerateSimpleResourceMojo) findConfiguredMojo("generateSimpleResource", pom);
        mojo.execute();

        File generated = file("META-INF/hello.txt");
        assertTrue(generated.exists());
        assertEquals(content(generated), "Hello!");
    }

}
