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
package io.micronaut.sourcegen.bytecode;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.HashMap;
import java.util.Map;

/**
 * The statement context.
 *
 * @param objectDef The current object definition
 * @param methodDef The current method definition.
 * @param locals    The locals
 */
@Internal
public record MethodContext(@Nullable ObjectDef objectDef,
                            MethodDef methodDef,
                            Map<String, Integer> locals) {

    public MethodContext(@Nullable ObjectDef objectDef,
                         MethodDef methodDef) {
        this(objectDef, methodDef, new HashMap<>());
    }

}
