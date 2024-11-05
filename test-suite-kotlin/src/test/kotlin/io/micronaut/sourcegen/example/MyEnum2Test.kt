package io.micronaut.sourcegen.example

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MyEnum2Test {

    @Test
    @Throws(Exception::class)
    fun test() {
        assertEquals(3, MyEnum2.entries.size)
        assertEquals("A", MyEnum2.A.myName())
        assertEquals("B", MyEnum2.B.myName())
        assertEquals("C", MyEnum2.C.myName())
    }

    @Test
    @Throws(Exception::class)
    fun testValues() {
        assertEquals(0, MyEnum2.A.myValue)
        assertEquals(1, MyEnum2.B.myValue)
        assertEquals(2, MyEnum2.C.myValue)
    }
}

