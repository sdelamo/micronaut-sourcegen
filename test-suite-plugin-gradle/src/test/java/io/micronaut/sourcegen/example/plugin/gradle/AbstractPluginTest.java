package io.micronaut.sourcegen.example.plugin.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class AbstractPluginTest {

    @TempDir
    public File baseDir;

    protected File settingsFile() {
        return baseDir.toPath().resolve("settings.gradle").toFile();
    }

    protected void settingsFile(String content) {
        try (FileOutputStream outputStream = new FileOutputStream(settingsFile())) {
            outputStream.write(content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected File buildFile() {
        return baseDir.toPath().resolve("build.gradle").toFile();
    }

    protected void buildFile(String content) {
        try (FileOutputStream outputStream = new FileOutputStream(buildFile())) {
            outputStream.write(content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected GradleRunner configureRunner(String ...args) {
        List<String> allArgs = new ArrayList<>();
        allArgs.add("--no-watch-fs");
        allArgs.add("-S");
        allArgs.add("-Porg.gradle.java.installations.auto-download=false");
        allArgs.add("-Porg.gradle.java.installations.auto-detect=false");
        allArgs.add("-Porg.gradle.java.installations.fromEnv=GRAALVM_HOME");
        allArgs.add("-Dio.micronaut.graalvm.rich.output=false");
        allArgs.addAll(Arrays.stream(args).toList());
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(baseDir)
                .withArguments(allArgs)
                .forwardStdOutput(new BufferedWriter(new OutputStreamWriter(System.out)))
                .forwardStdError(new BufferedWriter(new OutputStreamWriter(System.err)))
                .withDebug(true);
    }

    String micronautVersion() {
        return " \"" + System.getProperty("micronautVersion") + "\"";
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
