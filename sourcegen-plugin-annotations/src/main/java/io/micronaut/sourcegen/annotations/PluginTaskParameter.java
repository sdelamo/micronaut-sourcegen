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
 * An annotation that configures a parameter for plugin task.
 * Should be inside a type annotated with {@link PluginTaskConfig}.
 *
 * <p>The annotation is used during generation of particular plugin implementations, like Maven
 * Mojos or Gradle Tasks.</p>
 *
 * <p>Java primitives, lists and maps are supported.</p>
 * TODO support simple records and enums.
 *
 * @author Andriy Dmytruk
 * @since 1.5.x
 */
@Documented
@Retention(CLASS)
@Target({ ElementType.FIELD })
public @interface PluginTaskParameter {

    /**
     * Whether the parameter is required.
     * By default, parameters are not required to ensure a simple plugin API.
     * Parameters that have a default value should not be required.
     *
     * @return Whether it is required
     */
    boolean required() default false;

    /**
     * The default value.
     * Is allowed only for Java primitives or enums.
     *
     * @return The default value
     */
    String defaultValue() default "";

    /**
     * Whether the parameter is plugin-internal.
     * This means that the parameter won't get exposed as part of plugin API.
     * Specific logic will be written by developer in plugin to set the value for this parameter.
     * This is useful for parameters that depend on plugin-specific logic, like getting the
     * build directory.
     *
     * @return Whether it is internal
     */
    boolean internal() default false;

    /**
     * The property name for parameters generated in Maven Mojo.
     * It will fill in the {@code @Parameter(property='')} value.
     *
     * @return The property name
     */
    String mavenProperty() default "";

    /**
     * Whether the file is a directory.
     * Will only work for parameters of type {@link java.io.File}.
     *
     * @return Whether it is a directory.
     */
    boolean directory() default false;

}
