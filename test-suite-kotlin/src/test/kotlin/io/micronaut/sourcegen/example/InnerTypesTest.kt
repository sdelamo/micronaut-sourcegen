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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class InnerTypesTest {
    @Test
    fun enumTest() {
        Assertions.assertEquals(3, MyEnumWithInnerTypes.entries.size)
        Assertions.assertEquals("A", MyEnumWithInnerTypes.A.myName())
        Assertions.assertEquals("B", MyEnumWithInnerTypes.B.myName())
        Assertions.assertEquals("C", MyEnumWithInnerTypes.C.myName())

        innerEnumTest(
            MyEnumWithInnerTypes.InnerEnum.entries.toTypedArray().size,
            MyEnumWithInnerTypes.InnerEnum.SINGLE.myName(),
            MyEnumWithInnerTypes.InnerEnum.MARRIED.myName()
        )

        innerInterfaceTest()

        val innerRecord: MyEnumWithInnerTypes.InnerRecord = MyEnumWithInnerTypes.InnerRecord(3)
        Assertions.assertEquals(3, innerRecord.id)

        val innerClass: MyEnumWithInnerTypes.InnerClass =
            MyEnumWithInnerTypes.InnerClass("name")
        Assertions.assertEquals("name", innerClass.name)
    }

    @Test
    fun recordTest() {
        val myRecord: RecordWithInnerTypes = RecordWithInnerTypes(0, "name")
        Assertions.assertEquals(0, myRecord.id)
        Assertions.assertEquals("name", myRecord.name)

        innerEnumTest(
            RecordWithInnerTypes.InnerEnum.entries.toTypedArray().size,
            RecordWithInnerTypes.InnerEnum.SINGLE.myName(),
            RecordWithInnerTypes.InnerEnum.MARRIED.myName()
        )

        innerInterfaceTest()

        val innerRecord: RecordWithInnerTypes.InnerRecord = RecordWithInnerTypes.InnerRecord(3)
        Assertions.assertEquals(3, innerRecord.id)

        val innerClass: RecordWithInnerTypes.InnerClass =
            RecordWithInnerTypes.InnerClass("name")
        Assertions.assertEquals("name", innerClass.name)
    }

    @Test
    fun classTest() {
        val myClass: ClassWithInnerTypes = ClassWithInnerTypes(0, "name")
        Assertions.assertEquals(0, myClass.id)
        Assertions.assertEquals("name", myClass.name)

        innerEnumTest(
            ClassWithInnerTypes.InnerEnum.entries.toTypedArray().size,
            ClassWithInnerTypes.InnerEnum.SINGLE.myName(),
            ClassWithInnerTypes.InnerEnum.MARRIED.myName()
        )

        innerInterfaceTest()

        val innerRecord: ClassWithInnerTypes.InnerRecord = ClassWithInnerTypes.InnerRecord(3)
        Assertions.assertEquals(3, innerRecord.id)

        val innerClass: ClassWithInnerTypes.InnerClass =
            ClassWithInnerTypes.InnerClass("name")
        Assertions.assertEquals("name", innerClass.name)
    }

    @Test
    fun InterfaceTest() {
        Assertions.assertEquals(true, InterfaceWithInnerTypes.hello())

        innerEnumTest(
            InterfaceWithInnerTypes.InnerEnum.entries.toTypedArray().size,
            InterfaceWithInnerTypes.InnerEnum.SINGLE.myName(),
            InterfaceWithInnerTypes.InnerEnum.MARRIED.myName()
        )

        innerInterfaceTest()

        val innerRecord: InterfaceWithInnerTypes.InnerRecord = InterfaceWithInnerTypes.InnerRecord(3)
        Assertions.assertEquals(3, innerRecord.id)

        val innerClass: InterfaceWithInnerTypes.InnerClass =
            InterfaceWithInnerTypes.InnerClass("name")
        Assertions.assertEquals("name", innerClass.name)
    }

    private fun innerEnumTest(values: Int, single: String, married: String) {
        Assertions.assertEquals(2, values)
        Assertions.assertEquals("SINGLE", single)
        Assertions.assertEquals("MARRIED", married)
    }

    private fun innerInterfaceTest() {
        val myInstance = MyInstance()
        Assertions.assertEquals(123L, myInstance.findLong())
        myInstance.saveString("abc")
        Assertions.assertEquals("abc", myInstance.myString)
    }

    class MyInstance : MyEnumWithInnerTypes.InnerInterface,
        RecordWithInnerTypes.InnerInterface,
        ClassWithInnerTypes.InnerInterface,
        InterfaceWithInnerTypes.InnerInterface {
        var myString: String? = null

        override fun findLong(): Long {
            return 123L
        }

        override fun saveString(myString: String) {
            this.myString = myString
        }
    }
}
