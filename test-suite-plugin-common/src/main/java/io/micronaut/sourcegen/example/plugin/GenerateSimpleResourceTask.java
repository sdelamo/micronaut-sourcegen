/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.example.plugin;

import io.micronaut.sourcegen.annotations.PluginTask;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is a configuration for another plugin task run.
 * The properties are parameters and the single method defines the task execution.
 * The plugin generates a simple record.
 *
 * @param fileName The generated file name
 * @param content The content of the file
 * @param outputFolder The output folder
 */
@PluginTask
public record GenerateSimpleResourceTask(
    @PluginTaskParameter(required = true, globalProperty = "fileName")
    String fileName,
    @PluginTaskParameter(required = true, globalProperty = "content")
    String content,
    @PluginTaskParameter(output = true, directory = true, required = true)
    File outputFolder
) {

    /**
     * Generate a simple record in the supplied package and with the specified version.
     * This javadoc will be copied to the respected plugin implementations.
     */
    @PluginTaskExecutable
    public void generateSimpleResource() {
        System.out.println("Generating resource " + fileName);

        File outputFile = new File(outputFolder.getAbsolutePath() + File.separator + fileName);
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Finished resource " + fileName);
    }

}
