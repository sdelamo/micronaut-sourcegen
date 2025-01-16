package io.micronaut.sourcegen.example.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

public class TestMavenPluginTest extends AbstractMavenPluginTest {

    @Test
    void generateAndBuildSimpleRecord() throws Exception {
        File pom = new File("src/test/resources/test-pom.xml");

        TestMojo mojo = (TestMojo) findConfiguredMojo("generateSimpleRecord", pom);
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

}
