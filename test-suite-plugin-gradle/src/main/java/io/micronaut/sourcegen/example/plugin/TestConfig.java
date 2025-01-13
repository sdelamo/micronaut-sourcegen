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

import io.micronaut.sourcegen.annotations.PluginTaskConfig;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;

import java.util.List;
import java.util.Map;

@PluginTaskConfig
public record TestConfig(
    @PluginTaskParameter(required = true)
    String header,
    @PluginTaskParameter(defaultValue = "1")
    Integer repeatNum,
    @PluginTaskParameter(defaultValue = "<!-- Hello -->")
    String prefix,
    Map<String, String> contentMap,
    List<String> contentLines
) {

    @PluginTaskExecutable
    public void run() {
        System.out.println("Running test task!");
        System.out.flush();
    }

}
