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
package io.micronaut.sourcegen.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnnotatedTypeTest {

    private Validator validator;

    @Test
    public void testSuccess() {
        AnnotatedProperty annotatedProperty = new AnnotatedProperty(List.of(3f, 4f, 5f));
        List<Float> numbers = List.of(3f, 4f, 5f);
        assertEquals(numbers, annotatedProperty.numbers());
    }

    @Test
    public void testException() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        AnnotatedProperty annotatedProperty = new AnnotatedProperty(null);
        Set<ConstraintViolation<AnnotatedProperty>> violations = validator.validate(annotatedProperty);
        assertEquals(violations.size(), 3);

        annotatedProperty = new AnnotatedProperty(List.of());
        violations = validator.validate(annotatedProperty);
        assertEquals(violations.size(), 2);

        annotatedProperty = new AnnotatedProperty(List.of(100f));
        violations = validator.validate(annotatedProperty);
        assertEquals(violations.size(), 1);
    }
}
