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
package io.micronaut.sourcegen.example

import io.micronaut.core.annotation.Introspected
import io.micronaut.sourcegen.custom.example.CopyAnnotations
import kotlin.reflect.KClass

@CopyAnnotations(newTypeName = "BlueHeron")
@Heron.Simple(name = "A", age = 12, color = Heron.Color.WHITE)
@Heron.Nested(simple = Heron.Simple(name = "B", age = 12, color = Heron.Color.BLUE))
@Heron.MultiValue(
    simples = [Heron.Simple(name = "C"), Heron.Simple(name = "D")],
    values = [1, 2, 3],
    strings = ["a", "b"]
)
@Heron.Boo(name = "boom")
@Heron.Boo(name = "bam")
@Introspected
class Heron {
    enum class Color {
        BLUE,
        WHITE,
        BLACK
    }

    annotation class Simple(
        val name: String,
        val age: Int = 0,
        val color: Heron.Color = Heron.Color.BLACK,
        val type: KClass<*> = Void::class
    )

    annotation class Nested(val simple: Heron.Simple)

    annotation class MultiValue(val simples: Array<Heron.Simple>, val values: IntArray, val strings: Array<String>)

    @JvmRepeatable(Heron.BooRepeated::class)
    annotation class Boo(val name: String)

    annotation class BooRepeated(vararg val value: Heron.Boo)
}
