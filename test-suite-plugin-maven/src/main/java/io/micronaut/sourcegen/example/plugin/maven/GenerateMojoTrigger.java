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
package io.micronaut.sourcegen.example.plugin.maven;

import io.micronaut.sourcegen.annotations.GenerateMavenMojo;

/**
 * A class that triggers Maven Mojo generation.
 */
@GenerateMavenMojo(
    namePrefix = "AbstractGenerateSimpleRecord",
    micronautPlugin = false,
    source = "io.micronaut.sourcegen.example.plugin.GenerateSimpleRecordTask",
    mavenPropertyPrefix = "test.generate.simple.record"
)
@GenerateMavenMojo(
    namePrefix = "AbstractGenerateSimpleResource",
    micronautPlugin = false,
    source = "io.micronaut.sourcegen.example.plugin.GenerateSimpleResourceTask",
    mavenPropertyPrefix = "test.generate.simple.resource"
)
public final class GenerateMojoTrigger {
}
