package io.micronaut.sourcegen.model;

import io.micronaut.inject.ast.ClassElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ClassTypeDefTest {

    @Test
    void testEquality() {
        ClassTypeDef typeDef = new ClassTypeDef.JavaClass(String.class, false);
        ClassTypeDef className = new ClassTypeDef.ClassName(String.class.getName(), false);
        ClassTypeDef classTypeDef = new ClassTypeDef.ClassDefType(ClassDef.builder(String.class.getName()).build(), false);
        ClassTypeDef classElementType = new ClassTypeDef.ClassElementType(ClassElement.of(String.class), false);
        ClassTypeDef annotatedClassTypeDef = new ClassTypeDef.AnnotatedClassTypeDef(typeDef, List.of());
        ClassTypeDef parametrized = new ClassTypeDef.Parameterized(typeDef, List.of());

        Assertions.assertEquals(typeDef.hashCode(), classTypeDef.hashCode());
        Assertions.assertEquals(typeDef.hashCode(), className.hashCode());
        Assertions.assertEquals(typeDef.hashCode(), classElementType.hashCode());
        Assertions.assertEquals(typeDef.hashCode(), annotatedClassTypeDef.hashCode());
        Assertions.assertEquals(typeDef.hashCode(), parametrized.hashCode());

        Assertions.assertEquals(typeDef, classTypeDef);
        Assertions.assertEquals(typeDef, className);
        Assertions.assertEquals(typeDef, classElementType);
        Assertions.assertEquals(typeDef, annotatedClassTypeDef);
        // Parameterized not equals even with the zero param
        Assertions.assertNotEquals(typeDef, parametrized);

        Assertions.assertEquals(classTypeDef, typeDef);
        Assertions.assertEquals(className, typeDef);
        Assertions.assertEquals(classElementType, typeDef);
        Assertions.assertEquals(annotatedClassTypeDef, typeDef);

        // Parameterized not equals even with the zero param
        Assertions.assertNotEquals(parametrized, typeDef);
    }

    @Test
    void testNullabilityIsNotUsedForEquals() {
        ClassTypeDef typeDefNullable = new ClassTypeDef.JavaClass(String.class, true);
        ClassTypeDef typeDefNotNull = new ClassTypeDef.JavaClass(String.class, false);
        ClassTypeDef className = new ClassTypeDef.ClassName(String.class.getName(), false);
        ClassTypeDef classTypeDef = new ClassTypeDef.ClassDefType(ClassDef.builder(String.class.getName()).build(), false);
        ClassTypeDef classElementType = new ClassTypeDef.ClassElementType(ClassElement.of(String.class), false);
        ClassTypeDef annotatedClassTypeDef = new ClassTypeDef.AnnotatedClassTypeDef(typeDefNullable, List.of());

        Assertions.assertEquals(typeDefNullable.hashCode(), classTypeDef.hashCode());
        Assertions.assertEquals(typeDefNullable.hashCode(), className.hashCode());
        Assertions.assertEquals(typeDefNullable.hashCode(), classElementType.hashCode());
        Assertions.assertEquals(typeDefNullable.hashCode(), annotatedClassTypeDef.hashCode());

        Assertions.assertEquals(typeDefNotNull.hashCode(), classTypeDef.hashCode());
        Assertions.assertEquals(typeDefNotNull.hashCode(), className.hashCode());
        Assertions.assertEquals(typeDefNotNull.hashCode(), classElementType.hashCode());
        Assertions.assertEquals(typeDefNotNull.hashCode(), annotatedClassTypeDef.hashCode());

        Assertions.assertEquals(typeDefNullable, classTypeDef);
        Assertions.assertEquals(typeDefNullable, className);
        Assertions.assertEquals(typeDefNullable, classElementType);
        Assertions.assertEquals(typeDefNullable, annotatedClassTypeDef);

        Assertions.assertEquals(typeDefNotNull, classTypeDef);
        Assertions.assertEquals(typeDefNotNull, className);
        Assertions.assertEquals(typeDefNotNull, classElementType);
        Assertions.assertEquals(typeDefNotNull, annotatedClassTypeDef);

        Assertions.assertEquals(classTypeDef, typeDefNullable);
        Assertions.assertEquals(className, typeDefNullable);
        Assertions.assertEquals(classElementType, typeDefNullable);
        Assertions.assertEquals(annotatedClassTypeDef, typeDefNullable);

        Assertions.assertEquals(classTypeDef, typeDefNotNull);
        Assertions.assertEquals(className, typeDefNotNull);
        Assertions.assertEquals(classElementType, typeDefNotNull);
        Assertions.assertEquals(annotatedClassTypeDef, typeDefNotNull);

        Assertions.assertEquals(
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of()),
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of()).makeNullable()
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of()),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of()).makeNullable()
        );
        Assertions.assertEquals(
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)).makeNullable()
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)).makeNullable()
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), ClassTypeDef.STRING).makeNullable()
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of()).makeNullable()
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.OBJECT)).makeNullable()
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.CLASS)).makeNullable()
        );
    }

    @Test
    void testParametrized() {
        Assertions.assertEquals(
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of()),
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of())
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of()),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of())
        );
        Assertions.assertEquals(
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            new ClassTypeDef.Parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING))
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING))
        );
        Assertions.assertEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), ClassTypeDef.STRING)
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of())
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.OBJECT))
        );
        Assertions.assertNotEquals(
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.STRING)),
            TypeDef.parameterized(ClassTypeDef.of(List.class), List.of(ClassTypeDef.CLASS))
        );
    }

}
