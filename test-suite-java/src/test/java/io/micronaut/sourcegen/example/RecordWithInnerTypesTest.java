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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithInnerTypesTest {
    @Test
    public void initialTest() {
        RecordWithInnerTypes myRecord = new RecordWithInnerTypes(0, "name");
        assertEquals(0, myRecord.id());
        assertEquals("name", myRecord.name());
    }

    @Test
    public void innerEnumTest() {
        assertEquals(2, RecordWithInnerTypes.InnerEnum.values().length);
        assertEquals("SINGLE", RecordWithInnerTypes.InnerEnum.SINGLE.myName());
        assertEquals("MARRIED", RecordWithInnerTypes.InnerEnum.MARRIED.myName());
    }

    @Test
    public void innerRecordTest() {
        RecordWithInnerTypes.InnerRecord innerRecord = RecordWithInnerTypes$InnerRecordBuilder.builder().id(3).build();
        assertEquals(3, innerRecord.id());
    }

    @Test
    public void innerClassTest() {
        RecordWithInnerTypes.InnerClass innerClass = new RecordWithInnerTypes.InnerClass("name");
        assertEquals("name", innerClass.getName());
    }

    @Test
    public void innerInterfaceTest() {
        MyInstance myInstance = new MyInstance();
        Assertions.assertEquals(123L, myInstance.findLong());
        myInstance.saveString("abc");
        Assertions.assertEquals("abc", myInstance.myString);
    }

    static class MyInstance implements RecordWithInnerTypes.InnerInterface {

        String myString;

        @Override
        public Long findLong() {
            return 123L;
        }

        @Override
        public void saveString(String myString) {
            this.myString = myString;
        }
    }
}
