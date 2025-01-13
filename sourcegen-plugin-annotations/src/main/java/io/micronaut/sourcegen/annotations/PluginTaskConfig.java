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
 * An annotation that configures a plugin task.
 * The annotation is used for generation of particular plugin implementations, like Maven
 * Mojos or Gradle Tasks.
 *
 * @author Andriy Dmytruk
 * @since 1.5.x
 */
@Documented
@Retention(CLASS)
@Target({ ElementType.TYPE })
public @interface PluginTaskConfig {

    /**
     * The prefix to use for all names.
     * For example if the prefix is {@code Test}, task will be generated as {@code TestTask}.
     * The default is the annotated class name.
     *
     * @return The prefix
     */
    String namePrefix() default "";

    /**
     * The property prefix to use for parameters generated in Maven Mojo.
     *
     * @see PluginTaskParameter#mavenProperty()
     * @return The property prefix
     */
    String mavenPropertyPrefix() default "";

    /**
     * The gradle task group that will be set for all tasks.
     *
     * @return The task group
     */
    String gradleTaskGroup() default "";

    String gradleExtensionMethodName() default "";

    boolean micronautPlugin() default true;

    String dependency() default "";

}
