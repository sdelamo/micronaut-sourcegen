/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.example

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


@MicronautTest
internal class AnnotatedTypeTest(
    @Inject
    val validator: Validator
) {

    @Test
    fun testSuccess() {
        val annotatedProperty = AnnotatedProperty(listOf(3, 4, 5))
        val numbers = listOf(3, 4, 5)

        Assertions.assertEquals(numbers, annotatedProperty.numbers)
    }

    @Test
    fun testException() {
        var annotatedProperty = AnnotatedProperty(listOf())
        var violations = validator.validate(annotatedProperty)
        Assertions.assertEquals(0, violations.size)

        annotatedProperty = AnnotatedProperty(listOf(100, -10))
        violations = validator.validate(annotatedProperty)
        Assertions.assertEquals(2, violations.size)
    }

}
