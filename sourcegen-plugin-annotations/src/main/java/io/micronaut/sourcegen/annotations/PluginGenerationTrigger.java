/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.sourcegen.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * An annotation that triggers the generation of plugin sources.
 *
 * @author Andriy Dmytruk
 * @since 1.5.x
 */
@Documented
@Retention(CLASS)
@Target({ ElementType.TYPE })
public @interface PluginGenerationTrigger {

    /**
     * The plugin types to generate.
     */
    Type[] types();

    /**
     * The source configuration class that has {@link PluginTaskConfig} annotation.
     */
    String source();

    enum Type {
        MAVEN_MOJO,
        GRADLE_TASK,
        GRADLE_SPECIFICATION,
        GRADLE_EXTENSION,
        GRADLE_PLUGIN
    }
}
