package io.micronaut.sourcegen.generator.visitors;

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import spock.lang.Ignore;

class BuilderAnnotationVisitorSpec extends AbstractTypeElementSpec {

    @Ignore
    void "test builder"() {
        given:
        var classLoader = buildClassLoader("test.Walrus", """
        package test;
        import io.micronaut.sourcegen.annotations.PluginGenerationTrigger;
        import io.micronaut.sourcegen.annotations.PluginGenerationTrigger.Type;
        import io.micronaut.sourcegen.annotations.PluginTaskConfig;


        @PluginGenerationTrigger(type = Type.GRADLE_TASK)
        public record Trigger {
        }
        """)
        var walrusBuilderClass = classLoader.loadClass("test.WalrusBuilder")

        expect:
        var walrusBuilder = walrusBuilderClass.newInstance(new Object[]{})
        var walrus = walrusBuilder
                .name("Ted the Walrus")
                .age(1).build()
        walrus.name == "Ted the Walrus"
        walrus.age == 1
    }

}
