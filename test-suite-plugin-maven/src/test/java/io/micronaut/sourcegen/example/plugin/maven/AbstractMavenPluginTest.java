package io.micronaut.sourcegen.example.plugin.maven;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ResolverExpressionEvaluatorStub;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class AbstractMavenPluginTest extends AbstractMojoTestCase {

    @TempDir
    public File baseDir;

    public Mojo findConfiguredMojo(String goal, File configurationPom) throws Exception {
        Mojo mojo = lookupMojo(
            "io.micronaut.test",
            "test",
            "1.0.0",
            goal,
            null
        );
        PlexusConfiguration configuration = extractPluginConfiguration("test", configurationPom);
        configuration.addChild("outputFolder", baseDir.getAbsolutePath());
        ComponentConfigurator configurator = getContainer().lookup(ComponentConfigurator.class, "basic" );
        configurator.configureComponent(
            mojo,
            configuration,
            new ResolverExpressionEvaluatorStub(),
            getContainer().getContainerRealm()
        );
        return mojo;
    }

    File file(String relativePath) {
        return baseDir.toPath().resolve(relativePath).toFile();
    }

    String content(File file) {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
