package io.micronaut.sourcegen.bytecode;

import io.micronaut.context.BeanResolutionContext;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.custom.visitor.innerTypes.GenerateInnerTypeInEnumVisitor;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.EnumDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.lang.model.element.Modifier;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;

import static io.micronaut.sourcegen.bytecode.DecompilerUtils.decompileToJava;

class ByteCodeWriterTest {

    @Test
    void testDefaultPublicConstructor() {
        ClassDef def = ClassDef.builder("example.Example")
            .addModifiers(Modifier.PUBLIC)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(def, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();
        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x1
// signature Ljava/lang/Object;
// declaration: example/Example
public class example/Example {


  // access flags 0x1
  public <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

public class Example {
}
""", decompileToJava(bytes));
    }

    @Test
    void testDefaultPackagePrivateConstructor() {
        ClassDef def = ClassDef.builder("example.Example")
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(def, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();
        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Example
class example/Example {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Example {
}
""", decompileToJava(bytes));
    }

    @Test
    void testNullCondition() {
        ClassDef ifPredicateDef = ClassDef.builder("example.IfPredicate")
            .addMethod(MethodDef.builder("test").addParameter("param", TypeDef.OBJECT.makeNullable())
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .returns(boolean.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    methodParameters.get(0).isNull().doIf(TypeDef.Primitive.TRUE.returning()),
                    TypeDef.Primitive.FALSE.returning()
                )))
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(ifPredicateDef, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();
        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/IfPredicate
class example/IfPredicate {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public test(Ljava/lang/Object;)Z
   L0
    ALOAD 1
    IFNONNULL L1
    ICONST_1
    IRETURN
   L1
    ICONST_0
    IRETURN
   L2
    LOCALVARIABLE param Ljava/lang/Object; L0 L2 1
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class IfPredicate {
   public boolean test(Object param) {
      return param == null;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testBoxing() {
        ClassDef ifPredicateDef = ClassDef.builder("example.IfPredicate")
            .addMethod(MethodDef.builder("test").addParameter("param", TypeDef.of(Integer.class))
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .returns(int.class)
                .build((aThis, methodParameters) -> methodParameters.get(0).returning())
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(ifPredicateDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/IfPredicate
class example/IfPredicate {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public test(Ljava/lang/Integer;)I
   L0
    ALOAD 1
    CHECKCAST java/lang/Number
    INVOKEVIRTUAL java/lang/Number.intValue ()I
    IRETURN
   L1
    LOCALVARIABLE param Ljava/lang/Integer; L0 L1 1
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class IfPredicate {
   public int test(Integer param) {
      return ((Number)param).intValue();
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testUnboxing() {
        ClassDef ifPredicateDef = ClassDef.builder("example.IfPredicate")
            .addMethod(MethodDef.builder("test").addParameter("param", int.class)
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .returns(Integer.class)
                .build((aThis, methodParameters) -> methodParameters.get(0).returning())
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(ifPredicateDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/IfPredicate
class example/IfPredicate {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public test(I)Ljava/lang/Integer;
   L0
    ILOAD 1
    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
    ARETURN
   L1
    LOCALVARIABLE param I L0 L1 1
}
""", bytecode);


        Assertions.assertEquals("""
package example;

class IfPredicate {
   public Integer test(int param) {
      return param;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testEnum() {
        EnumDef myEnum = EnumDef.builder("MyEnum").addEnumConstant("A").addEnumConstant("B").addEnumConstant("C").build();

        String bytecode = toBytecode(myEnum);
        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x4010
// signature Ljava/lang/Enum<LMyEnum;>;
// declaration: MyEnum extends java.lang.Enum<MyEnum>
final enum MyEnum extends java/lang/Enum {


  // access flags 0x4019
  public final static enum LMyEnum; A

  // access flags 0x4019
  public final static enum LMyEnum; B

  // access flags 0x4019
  public final static enum LMyEnum; C

  // access flags 0xA
  private static [LMyEnum; $VALUES

  // access flags 0x8
  static <clinit>()V
    NEW MyEnum
    DUP
    LDC "A"
    ICONST_0
    INVOKESPECIAL MyEnum.<init> (Ljava/lang/String;I)V
    PUTSTATIC MyEnum.A : LMyEnum;
    NEW MyEnum
    DUP
    LDC "B"
    ICONST_1
    INVOKESPECIAL MyEnum.<init> (Ljava/lang/String;I)V
    PUTSTATIC MyEnum.B : LMyEnum;
    NEW MyEnum
    DUP
    LDC "C"
    ICONST_2
    INVOKESPECIAL MyEnum.<init> (Ljava/lang/String;I)V
    PUTSTATIC MyEnum.C : LMyEnum;
    INVOKESTATIC MyEnum.$values ()[LMyEnum;
    PUTSTATIC MyEnum.$VALUES : [LMyEnum;
    RETURN

  // access flags 0x2
  private <init>(Ljava/lang/String;I)V
   L0
    ALOAD 0
    ALOAD 1
    ILOAD 2
    INVOKESPECIAL java/lang/Enum.<init> (Ljava/lang/String;I)V
    RETURN
   L1
    LOCALVARIABLE arg0 Ljava/lang/String; L0 L1 1
    LOCALVARIABLE arg1 I L0 L1 2

  // access flags 0xA
  private static $values()[LMyEnum;
    ICONST_3
    ANEWARRAY MyEnum
    DUP
    ICONST_0
    GETSTATIC MyEnum.A : LMyEnum;
    AASTORE
    DUP
    ICONST_1
    GETSTATIC MyEnum.B : LMyEnum;
    AASTORE
    DUP
    ICONST_2
    GETSTATIC MyEnum.C : LMyEnum;
    AASTORE
    ARETURN

  // access flags 0x9
  public static values()[LMyEnum;
    GETSTATIC MyEnum.$VALUES : [LMyEnum;
    INVOKEVIRTUAL [LMyEnum;.clone ()Ljava/lang/Object;
    CHECKCAST [LMyEnum;
    ARETURN

  // access flags 0x9
  public static valueOf(Ljava/lang/String;)LMyEnum;
   L0
    LDC LMyEnum;.class
    ALOAD 0
    INVOKESTATIC java/lang/Enum.valueOf (Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    CHECKCAST MyEnum
    ARETURN
   L1
    LOCALVARIABLE value Ljava/lang/String; L0 L1 1
}
""", bytecode);
    }

    @Test
    void testSynchronized() {
        FieldDef beanResolutionContextField = FieldDef.builder("$beanResolutionContext", BeanResolutionContext.class)
            .addModifiers(Modifier.PRIVATE)
            .build();
        FieldDef targetField = FieldDef.builder("target", ClassTypeDef.of("test.SomeTarget")).addModifiers(Modifier.PRIVATE).build();

        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addField(beanResolutionContextField)
            .addField(targetField)
            .addMethod(
            MethodDef.builder("interceptedTarget")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeDef.OBJECT)
                .build((aThis, methodParameters) -> {
                    VariableDef.Field targetFieldAccess = aThis.field(targetField);
                    return StatementDef.multi(
                        targetFieldAccess.newLocal("target", targetVar ->
                            targetVar.ifNull(
                                new StatementDef.Synchronized(
                                    aThis,
                                    StatementDef.multi(
                                        targetVar.assign(targetFieldAccess),
                                        targetVar.ifNull(
                                            StatementDef.multi(
                                                targetFieldAccess.assign(ExpressionDef.nullValue()),
                                                aThis.field(beanResolutionContextField).assign(ExpressionDef.nullValue())
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        targetFieldAccess.returning()
                    );
                })
        ).build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {


  // access flags 0x2
  private Lio/micronaut/context/BeanResolutionContext; $beanResolutionContext

  // access flags 0x2
  private Ltest/SomeTarget; target

  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public interceptedTarget()Ljava/lang/Object;
   L0
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ASTORE 1
    ALOAD 1
    IFNONNULL L1
    TRYCATCHBLOCK L2 L3 L4 null
    TRYCATCHBLOCK L4 L5 L4 null
    ALOAD 0
    DUP
    ASTORE 2
    MONITORENTER
   L2
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ASTORE 1
    ALOAD 1
    IFNONNULL L6
    ALOAD 0
    ACONST_NULL
    PUTFIELD test/MyClass.target : Ltest/SomeTarget;
    ALOAD 0
    ACONST_NULL
    PUTFIELD test/MyClass.$beanResolutionContext : Lio/micronaut/context/BeanResolutionContext;
   L6
    ALOAD 2
    MONITOREXIT
   L3
    GOTO L7
   L4
    ASTORE 3
    ALOAD 2
    MONITOREXIT
   L5
    ALOAD 3
    ATHROW
   L7
   L1
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ARETURN
   L8
    LOCALVARIABLE target Ltest/SomeTarget; L0 L8 1
}
""", bytecode);


        Assertions.assertEquals("""
package test;

import io.micronaut.context.BeanResolutionContext;

class MyClass {
   private BeanResolutionContext $beanResolutionContext;
   private SomeTarget target;

   public Object interceptedTarget() {
      SomeTarget target = this.target;
      if (target == null) {
         synchronized(this) {
            target = this.target;
            if (target == null) {
               this.target = null;
               this.$beanResolutionContext = null;
            }
         }
      }

      return this.target;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testFinally() {
        FieldDef myField = FieldDef.builder("myField", Integer.class)
            .addModifiers(Modifier.PRIVATE)
            .build();

        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addField(myField)
            .addMethod(
            MethodDef.builder("interceptedTarget")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeDef.OBJECT)
                .build((aThis, methodParameters) -> {
                    VariableDef.Field myFieldAccess = aThis.field(myField);
                    return StatementDef.multi(
                        myFieldAccess.ifNull(
                            ExpressionDef.constant(1).returning()
                        ),
                        myFieldAccess.ifNonNull(
                            ExpressionDef.constant(2).returning()
                        ),
                        myFieldAccess.returning()
                    ).doTry().doFinally(ClassTypeDef.of(System.class)
                        .getStaticField("out", TypeDef.of(PrintStream.class))
                        .invoke("println", TypeDef.VOID, ExpressionDef.constant("Hello")));
                })
        ).build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {


  // access flags 0x2
  private Ljava/lang/Integer; myField

  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public interceptedTarget()Ljava/lang/Object;
    TRYCATCHBLOCK L0 L1 L2 null
   L0
    ALOAD 0
    GETFIELD test/MyClass.myField : Ljava/lang/Integer;
    IFNONNULL L3
    ICONST_1
    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
    ASTORE 1
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 1
    ARETURN
   L3
    ALOAD 0
    GETFIELD test/MyClass.myField : Ljava/lang/Integer;
    IFNULL L4
    ICONST_2
    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
    ASTORE 2
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 2
    ARETURN
   L4
    ALOAD 0
    GETFIELD test/MyClass.myField : Ljava/lang/Integer;
    ASTORE 3
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 3
    ARETURN
   L1
    GOTO L5
   L2
    ASTORE 4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 4
    ATHROW
    GOTO L5
   L5
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   private Integer myField;

   public Object interceptedTarget() {
      try {
         if (this.myField == null) {
            Integer var1 = 1;
            System.out.println("Hello");
            return var1;
         } else if (this.myField != null) {
            Integer var2 = 2;
            System.out.println("Hello");
            return var2;
         } else {
            Integer var3 = this.myField;
            System.out.println("Hello");
            return var3;
         }
      } finally {
         System.out.println("Hello");
      }
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testFinally2 () {
        FieldDef targetField = FieldDef.builder("target", Object.class)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();

        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addField(targetField)
            .addMethod(
                MethodDef.builder("swap")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(Object.class)
                    .returns(Object.class)
                    .build((aThis, methodParameters) -> StatementDef.multi(
                        ClassTypeDef.of(System.class)
                            .getStaticField("out", TypeDef.of(PrintStream.class))
                            .invoke("println", TypeDef.VOID, ExpressionDef.constant("Hello")),
                        StatementDef.doTry(
                            aThis.field(targetField).newLocal("target", targetVar -> StatementDef.multi(
                                aThis.field(targetField).assign(methodParameters.get(0)),
                                targetVar.returning()
                            ))
                        ).doFinally(ClassTypeDef.of(System.class)
                            .getStaticField("out", TypeDef.of(PrintStream.class))
                            .invoke("println", TypeDef.VOID, ExpressionDef.constant("World")))
                    ))
        ).build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {


  // access flags 0x12
  private final Ljava/lang/Object; target

  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public swap(Ljava/lang/Object;)Ljava/lang/Object;
   L0
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    TRYCATCHBLOCK L1 L2 L3 null
   L1
   L4
    ALOAD 0
    GETFIELD test/MyClass.target : Ljava/lang/Object;
    ASTORE 2
    ALOAD 0
    ALOAD 1
    PUTFIELD test/MyClass.target : Ljava/lang/Object;
    ALOAD 2
    ASTORE 3
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "World"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 3
    ARETURN
   L5
    LOCALVARIABLE target Ljava/lang/Object; L4 L5 2
   L2
    GOTO L6
   L3
    ASTORE 4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "World"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 4
    ATHROW
    GOTO L6
   L6
   L7
    LOCALVARIABLE arg1 Ljava/lang/Object; L0 L7 1
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   private final Object target;

   public Object swap(Object arg1) {
      System.out.println("Hello");

      try {
         Object target = this.target;
         this.target = arg1;
         System.out.println("World");
         return target;
      } finally {
         System.out.println("World");
      }
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testTableSwitchWithDuplicateStatements() {
        StatementDef sameStatement = TypeDef.Primitive.INT.constant(123).returning();
        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addMethod(
                MethodDef.builder("swap")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(int.class)
                    .returns(int.class)
                    .build((aThis, methodParameters) -> methodParameters.get(0)
                        .asStatementSwitch(
                            TypeDef.Primitive.INT,
                            Map.of(
                                ExpressionDef.constant(1), sameStatement,
                                ExpressionDef.constant(2), TypeDef.Primitive.INT.constant(111).returning(),
                                ExpressionDef.constant(3), sameStatement,
                                ExpressionDef.constant(4), sameStatement,
                                ExpressionDef.constant(5), sameStatement,
                                ExpressionDef.constant(6), sameStatement,
                                ExpressionDef.constant(7), sameStatement
                            ),
                            TypeDef.Primitive.INT.constant(444).returning()
                        )
                    )
        ).build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public swap(I)I
   L0
    ILOAD 1
    TABLESWITCH
      1: L1
      2: L2
      3: L1
      4: L1
      5: L1
      6: L1
      7: L1
      default: L3
   L1
    BIPUSH 123
    IRETURN
    GOTO L4
   L2
    BIPUSH 111
    IRETURN
    GOTO L4
   L3
    SIPUSH 444
    IRETURN
   L4
   L5
    LOCALVARIABLE arg1 I L0 L5 1
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   public int swap(int arg1) {
      switch (arg1) {
         case 1:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
            return 123;
         case 2:
            return 111;
         default:
            return 444;
      }
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testTableLookupWithDuplicateStatements() {
        StatementDef sameStatement = TypeDef.Primitive.INT.constant(123).returning();
        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addMethod(
                MethodDef.builder("swap")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(int.class)
                    .returns(int.class)
                    .build((aThis, methodParameters) -> methodParameters.get(0)
                        .asStatementSwitch(
                            TypeDef.Primitive.INT,
                            Map.of(
                                ExpressionDef.constant(1), sameStatement,
                                ExpressionDef.constant(20), TypeDef.Primitive.INT.constant(111).returning(),
                                ExpressionDef.constant(30), sameStatement,
                                ExpressionDef.constant(40), sameStatement,
                                ExpressionDef.constant(50), sameStatement,
                                ExpressionDef.constant(60), sameStatement,
                                ExpressionDef.constant(70), sameStatement
                            ),
                            TypeDef.Primitive.INT.constant(444).returning()
                        )
                    )
        ).build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x1
  public swap(I)I
   L0
    ILOAD 1
    LOOKUPSWITCH
      1: L1
      20: L2
      30: L1
      40: L1
      50: L1
      60: L1
      70: L1
      default: L3
   L1
    BIPUSH 123
    IRETURN
    GOTO L4
   L2
    BIPUSH 111
    IRETURN
    GOTO L4
   L3
    SIPUSH 444
    IRETURN
   L4
   L5
    LOCALVARIABLE arg1 I L0 L5 1
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   public int swap(int arg1) {
      switch (arg1) {
         case 1:
         case 30:
         case 40:
         case 50:
         case 60:
         case 70:
            return 123;
         case 20:
            return 111;
         default:
            return 444;
      }
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testInnerClass() {
        ClassDef classDef = ClassDef.builder("test.MyClass")
            .addMethod(
                MethodDef.builder("hello")
                    .build((aThis, methodParameters) -> ExpressionDef.constant("world").returning())
            ).addInnerType(ClassDef.builder("Inner")
                .addMethod(
                    MethodDef.builder("hi")
                        .build((aThis, methodParameters) -> ExpressionDef.constant("hello").returning())
                ).build())
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: test/MyClass
class test/MyClass {

  // access flags 0x9
  public static INNERCLASS test/MyClass$Inner test/MyClass Inner
  NESTMEMBER test/MyClass$Inner

  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  hello()Ljava/lang/String;
    LDC "world"
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   String hello() {
      return "world";
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testInnerEnum() {
        EnumDef enumDef = GenerateInnerTypeInEnumVisitor.getEnumDef("example", VisitorContext.Language.JAVA);

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(enumDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x4011
// signature Ljava/lang/Enum<Lexample/MyEnumWithInnerTypes;>;
// declaration: example/MyEnumWithInnerTypes extends java.lang.Enum<example.MyEnumWithInnerTypes>
public final enum example/MyEnumWithInnerTypes extends java/lang/Enum {

  // access flags 0x4019
  public final static enum INNERCLASS example/MyEnumWithInnerTypes$InnerEnum example/MyEnumWithInnerTypes InnerEnum
  NESTMEMBER example/MyEnumWithInnerTypes$InnerEnum
  // access flags 0x9
  public static INNERCLASS example/MyEnumWithInnerTypes$InnerRecord example/MyEnumWithInnerTypes InnerRecord
  NESTMEMBER example/MyEnumWithInnerTypes$InnerRecord
  // access flags 0x9
  public static INNERCLASS example/MyEnumWithInnerTypes$InnerClass example/MyEnumWithInnerTypes InnerClass
  NESTMEMBER example/MyEnumWithInnerTypes$InnerClass
  // access flags 0x609
  public static abstract INNERCLASS example/MyEnumWithInnerTypes$InnerInterface example/MyEnumWithInnerTypes InnerInterface
  NESTMEMBER example/MyEnumWithInnerTypes$InnerInterface

  // access flags 0x4019
  public final static enum Lexample/MyEnumWithInnerTypes; A

  // access flags 0x4019
  public final static enum Lexample/MyEnumWithInnerTypes; B

  // access flags 0x4019
  public final static enum Lexample/MyEnumWithInnerTypes; C

  // access flags 0xA
  private static [Lexample/MyEnumWithInnerTypes; $VALUES

  // access flags 0x8
  static <clinit>()V
    NEW example/MyEnumWithInnerTypes
    DUP
    LDC "A"
    ICONST_0
    INVOKESPECIAL example/MyEnumWithInnerTypes.<init> (Ljava/lang/String;I)V
    PUTSTATIC example/MyEnumWithInnerTypes.A : Lexample/MyEnumWithInnerTypes;
    NEW example/MyEnumWithInnerTypes
    DUP
    LDC "B"
    ICONST_1
    INVOKESPECIAL example/MyEnumWithInnerTypes.<init> (Ljava/lang/String;I)V
    PUTSTATIC example/MyEnumWithInnerTypes.B : Lexample/MyEnumWithInnerTypes;
    NEW example/MyEnumWithInnerTypes
    DUP
    LDC "C"
    ICONST_2
    INVOKESPECIAL example/MyEnumWithInnerTypes.<init> (Ljava/lang/String;I)V
    PUTSTATIC example/MyEnumWithInnerTypes.C : Lexample/MyEnumWithInnerTypes;
    INVOKESTATIC example/MyEnumWithInnerTypes.$values ()[Lexample/MyEnumWithInnerTypes;
    PUTSTATIC example/MyEnumWithInnerTypes.$VALUES : [Lexample/MyEnumWithInnerTypes;
    RETURN

  // access flags 0x2
  private <init>(Ljava/lang/String;I)V
   L0
    ALOAD 0
    ALOAD 1
    ILOAD 2
    INVOKESPECIAL java/lang/Enum.<init> (Ljava/lang/String;I)V
    RETURN
   L1
    LOCALVARIABLE arg0 Ljava/lang/String; L0 L1 1
    LOCALVARIABLE arg1 I L0 L1 2

  // access flags 0xA
  private static $values()[Lexample/MyEnumWithInnerTypes;
    ICONST_3
    ANEWARRAY example/MyEnumWithInnerTypes
    DUP
    ICONST_0
    GETSTATIC example/MyEnumWithInnerTypes.A : Lexample/MyEnumWithInnerTypes;
    AASTORE
    DUP
    ICONST_1
    GETSTATIC example/MyEnumWithInnerTypes.B : Lexample/MyEnumWithInnerTypes;
    AASTORE
    DUP
    ICONST_2
    GETSTATIC example/MyEnumWithInnerTypes.C : Lexample/MyEnumWithInnerTypes;
    AASTORE
    ARETURN

  // access flags 0x9
  public static values()[Lexample/MyEnumWithInnerTypes;
    GETSTATIC example/MyEnumWithInnerTypes.$VALUES : [Lexample/MyEnumWithInnerTypes;
    INVOKEVIRTUAL [Lexample/MyEnumWithInnerTypes;.clone ()Ljava/lang/Object;
    CHECKCAST [Lexample/MyEnumWithInnerTypes;
    ARETURN

  // access flags 0x9
  public static valueOf(Ljava/lang/String;)Lexample/MyEnumWithInnerTypes;
   L0
    LDC Lexample/MyEnumWithInnerTypes;.class
    ALOAD 0
    INVOKESTATIC java/lang/Enum.valueOf (Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    CHECKCAST example/MyEnumWithInnerTypes
    ARETURN
   L1
    LOCALVARIABLE value Ljava/lang/String; L0 L1 1

  // access flags 0x1
  public myName()Ljava/lang/String;
    ALOAD 0
    INVOKEVIRTUAL example/MyEnumWithInnerTypes.toString ()Ljava/lang/String;
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

public enum MyEnumWithInnerTypes {
   A,
   B,
   C;

   private static MyEnumWithInnerTypes[] $VALUES = $values();

   private static MyEnumWithInnerTypes[] $values() {
      return new MyEnumWithInnerTypes[]{A, B, C};
   }

   public String myName() {
      return this.toString();
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testLocalVariable() {
        MethodDef method = MethodDef.builder("test").addParameter("myIn", String.class)
            .returns(int.class)
            .build((aThis, methodParameters) -> {
                return methodParameters.get(0).invokeHashCode().newLocal("hc", hcVar -> {
                    return hcVar.math("+", TypeDef.Primitive.INT.constant(4)).returning();
                });
            });
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(method)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  test(Ljava/lang/String;)I
   L0
   L1
    ALOAD 1
    IFNONNULL L2
    ICONST_0
    GOTO L3
   L2
    ALOAD 1
    INVOKEVIRTUAL java/lang/String.hashCode ()I
   L3
    ISTORE 2
    ILOAD 2
    ICONST_4
    IADD
    IRETURN
   L4
    LOCALVARIABLE myIn Ljava/lang/String; L0 L4 1
    LOCALVARIABLE hc I L1 L4 2
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   int test(String myIn) {
      int hc = myIn == null ? 0 : myIn.hashCode();
      return hc + 4;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testMultipleCasts() {
        MethodDef method = MethodDef.builder("test").addParameter("myIn", Object.class)
            .returns(Integer.class)
            .build((aThis, methodParameters) -> methodParameters.get(0).cast(TypeDef.Primitive.INT).cast(TypeDef.of(Integer.class)).returning());
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(method)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  test(Ljava/lang/Object;)Ljava/lang/Integer;
   L0
    ALOAD 1
    CHECKCAST java/lang/Integer
    ARETURN
   L1
    LOCALVARIABLE myIn Ljava/lang/Object; L0 L1 1
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   Integer test(Object myIn) {
      return (Integer)myIn;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testNullCast() {
        MethodDef method = MethodDef.builder("test").addParameter("myIn", Object.class)
            .returns(Integer.class)
            .build((aThis, methodParameters) -> ExpressionDef.nullValue().cast(TypeDef.of(Integer.class)).returning());
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(method)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  test(Ljava/lang/Object;)Ljava/lang/Integer;
   L0
    ACONST_NULL
    ARETURN
   L1
    LOCALVARIABLE myIn Ljava/lang/Object; L0 L1 1
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   Integer test(Object myIn) {
      return null;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testTrueFalseConditions() {
        MethodDef method = MethodDef.builder("test")
            .returns(boolean.class)
            .build((aThis, methodParameters) ->
                new ExpressionDef.Or(
                    ExpressionDef.trueValue().isTrue().and(ExpressionDef.falseValue().isTrue()),
                    ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue())
                ).returning());
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(method)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  test()Z
    ICONST_1
    ICONST_1
    IF_ICMPNE L0
    ICONST_0
    ICONST_1
    IF_ICMPNE L0
    GOTO L1
   L0
    ICONST_1
    ICONST_1
    IF_ICMPEQ L1
    ICONST_0
    ICONST_1
    IF_ICMPEQ L1
    GOTO L2
   L1
    ICONST_1
    GOTO L3
   L2
    ICONST_0
   L3
    IRETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   boolean test() {
      return true && false || true || false;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testTrueFalseConditions2() {
        MethodDef method = MethodDef.builder("test")
            .returns(boolean.class)
            .build((aThis, methodParameters) ->
                new ExpressionDef.And(
                    ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue()),
                    ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue())
                ).returning());
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(method)
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);
        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  test()Z
    ICONST_1
    ICONST_1
    IF_ICMPEQ L0
    ICONST_0
    ICONST_1
    IF_ICMPEQ L0
    GOTO L1
   L0
    ICONST_1
    ICONST_1
    IF_ICMPEQ L2
    ICONST_0
    ICONST_1
    IF_ICMPEQ L2
    GOTO L1
   L2
    ICONST_1
    GOTO L3
   L1
    ICONST_0
   L3
    IRETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   boolean test() {
      return (true || false) && (true || false);
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testPrimitives() {

        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(MethodDef.builder("testLong")
                .returns(TypeDef.Primitive.LONG)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.LONG.constant(123445).returning()
                )
            )
            .addMethod(MethodDef.builder("testInt")
                .returns(TypeDef.Primitive.INT)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.INT.constant(334455).returning()
                )
            )
            .addMethod(MethodDef.builder("testFloat")
                .returns(TypeDef.Primitive.FLOAT)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.FLOAT.constant(123.456).returning()
                )
            )
            .addMethod(MethodDef.builder("testShort")
                .returns(TypeDef.Primitive.SHORT)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.SHORT.constant(345).returning()
                )
            )
            .addMethod(MethodDef.builder("testChar")
                .returns(TypeDef.Primitive.CHAR)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.CHAR.constant('c').returning()
                )
            )
            .addMethod(MethodDef.builder("testBoolean")
                .returns(TypeDef.Primitive.BOOLEAN)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.BOOLEAN.constant(true).returning()
                )
            )
            .addMethod(MethodDef.builder("testByte")
                .returns(TypeDef.Primitive.BYTE)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.BYTE.constant(45).returning()
                )
            )
            .addMethod(MethodDef.builder("testDouble")
                .returns(TypeDef.Primitive.DOUBLE)
                .build((aThis, methodParameters) ->
                        TypeDef.Primitive.DOUBLE.constant(444.555).returning()
                )
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);

        Assertions.assertEquals("""
package example;

class Test {
   long testLong() {
      return 123445L;
   }

   int testInt() {
      return 334455;
   }

   float testFloat() {
      return 123.456F;
   }

   short testShort() {
      return 345;
   }

   char testChar() {
      return 'c';
   }

   boolean testBoolean() {
      return true;
   }

   byte testByte() {
      return 45;
   }

   double testDouble() {
      return 444.555;
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testCastingNull() {

        MethodDef build = MethodDef.builder("acceptValue")
            .addParameter(TypeDef.of(Integer.class))
            .returns(TypeDef.OBJECT)
            .build((aThis, methodParameters) -> methodParameters.get(0).returning());
        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(build
            )
            .addMethod(MethodDef.builder("invoke")
                .build((aThis, methodParameters) ->
                    aThis.invoke(build, ExpressionDef.nullValue()).returning()
                )
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  acceptValue(Ljava/lang/Integer;)Ljava/lang/Object;
   L0
    ALOAD 1
    ARETURN
   L1
    LOCALVARIABLE arg1 Ljava/lang/Integer; L0 L1 1

  // access flags 0x0
  invoke()Ljava/lang/Object;
    ALOAD 0
    ACONST_NULL
    INVOKEVIRTUAL example/Test.acceptValue (Ljava/lang/Integer;)Ljava/lang/Object;
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   Object acceptValue(Integer arg1) {
      return arg1;
   }

   Object invoke() {
      return this.acceptValue((Integer)null);
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testCastingClassDefWithSuperclass() {

        ClassDef myList = ClassDef.builder("example.MyList")
            .superclass(ClassTypeDef.of(AbstractList.class))
            .build();

        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(MethodDef.builder("load")
                .returns(ClassTypeDef.of(List.class))
                .build((aThis, methodParameters) -> myList.asTypeDef()
                    .instantiate().returning())
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  load()Ljava/util/List;
    NEW example/MyList
    DUP
    INVOKESPECIAL example/MyList.<init> ()V
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

import java.util.List;

class Test {
   List load() {
      return new MyList();
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testCastingThisClassDefWithSuperclass() {

        ClassDef classDef = ClassDef.builder("example.Test")
            .superclass(TypeDef.parameterized(AbstractList.class, Number.class))
            .addMethod(MethodDef.builder("load")
                .returns(ClassTypeDef.of(List.class))
                .build((aThis, methodParameters) -> aThis.type().instantiate().returning())
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/util/AbstractList<Ljava/lang/Number;>;
// declaration: example/Test extends java.util.AbstractList<java.lang.Number>
class example/Test extends java/util/AbstractList {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/util/AbstractList.<init> ()V
    RETURN

  // access flags 0x0
  load()Ljava/util/List;
    NEW example/Test
    DUP
    INVOKESPECIAL example/Test.<init> ()V
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

import java.util.AbstractList;
import java.util.List;

class Test extends AbstractList {
   List load() {
      return new Test();
   }
}
""", decompileToJava(bytes));
    }

    @Test
    void testCastingEnum() {

        EnumDef enumDef = EnumDef.builder("example.MyEnum")
            .addEnumConstant("A")
            .addEnumConstant("B")
            .build();

        ClassDef classDef = ClassDef.builder("example.Test")
            .addMethod(MethodDef.builder("load")
                .returns(Enum.class)
                .build((aThis, methodParameters) -> enumDef.asTypeDef()
                    .getStaticField(enumDef.getField("A"))
                    .returning())
            )
            .build();

        StringWriter bytecodeWriter = new StringWriter();
        byte[] bytes = generateFile(classDef, bytecodeWriter);

        String bytecode = bytecodeWriter.toString();

        Assertions.assertEquals("""
// class version 61.0 (61)
// access flags 0x0
// signature Ljava/lang/Object;
// declaration: example/Test
class example/Test {


  // access flags 0x0
  <init>()V
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
    RETURN

  // access flags 0x0
  load()Ljava/lang/Enum;
    GETSTATIC example/MyEnum.A : Lexample/MyEnum;
    ARETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class Test {
   Enum load() {
      return MyEnum.A;
   }
}
""", decompileToJava(bytes));
    }

    private String toBytecode(ObjectDef objectDef) {
        StringWriter stringWriter = new StringWriter();
        generateFile(objectDef, stringWriter);
        return stringWriter.toString();
    }

    private byte[] generateFile(ObjectDef objectDef, StringWriter stringWriter) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        CheckClassAdapter checkClassAdapter = new CheckClassAdapter(classWriter);
        TraceClassVisitor tcv = new TraceClassVisitor(checkClassAdapter, new PrintWriter(stringWriter));

        new ByteCodeWriter(false, false).writeObject(tcv, objectDef);

        tcv.visitEnd();

        return classWriter.toByteArray();
    }

}
