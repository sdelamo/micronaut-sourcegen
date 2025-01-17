/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.sourcegen.example;


import io.micronaut.sourcegen.custom.example.*;

import java.util.List;

@GenerateMyBean1
@GenerateMyBean2
@GenerateMyBean3
@GenerateMyRecord1
@GenerateMyRecord3
@GenerateInterface
@GenerateMyRepository1
@GenerateMyEnum1
@GenerateIfsPredicate
@GenerateSwitch
@GenerateArray
@GenerateAnnotatedType
@GenerateInnerTypes
@GenerateMyEnum2
public class Trigger {
    public List<String> copyAddresses;
}
