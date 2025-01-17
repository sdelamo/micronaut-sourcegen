/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.sourcegen.example.plugin.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * An extension of the generated mojo that configures the output folder.
 */
@Mojo(name = "generateSimpleRecord")
public class GenerateSimpleRecordMojo extends AbstractGenerateSimpleRecordMojo {

    @Parameter(
        required = true,
        defaultValue = "${project.build.directory}/generated/simpleRecord"
    )
    private File outputFolder;

    @Parameter(property = "generate.simple.record.enabled", defaultValue = "true")
    private boolean enabled;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    protected boolean isEnabled() {
        return enabled;
    }

    @Override
    protected File getOutputFolder() {
        return outputFolder;
    }

    @Override
    public void execute() {
        if (project != null) {
            project.addCompileSourceRoot(
                new File(outputFolder, "src/main/java".replace("/", File.separator)).getAbsolutePath()
            );
        }
        super.execute();
    }
}
