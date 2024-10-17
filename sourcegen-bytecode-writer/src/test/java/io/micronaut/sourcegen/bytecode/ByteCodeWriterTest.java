package io.micronaut.sourcegen.bytecode;

import io.micronaut.context.BeanResolutionContext;
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
import java.util.Map;

import static io.micronaut.sourcegen.bytecode.DecompilerUtils.decompileToJava;

class ByteCodeWriterTest {

    @Test
    void testNullCondition() {
        ClassDef ifPredicateDef = ClassDef.builder("example.IfPredicate")
            .addMethod(MethodDef.builder("test").addParameter("param", TypeDef.OBJECT.makeNullable())
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .returns(boolean.class)
                .build((aThis, methodParameters) -> StatementDef.multi(
                    methodParameters.get(0).isNull().asConditionIf(TypeDef.Primitive.TRUE.returning()),
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
    ALOAD 1
    IFNONNULL L0
    ICONST_1
    IRETURN
   L0
    ICONST_0
    IRETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class IfPredicate {
   public boolean test(Object var1) {
      return var1 == null;
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
    ALOAD 1
    CHECKCAST java/lang/Number
    INVOKEVIRTUAL java/lang/Number.intValue ()I
    IRETURN
}
""", bytecode);

        Assertions.assertEquals("""
package example;

class IfPredicate {
   public int test(Integer var1) {
      return ((Number)var1).intValue();
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
    ILOAD 1
    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
    ARETURN
}
""", bytecode);


        Assertions.assertEquals("""
package example;

class IfPredicate {
   public Integer test(int var1) {
      return var1;
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

  // access flags 0x0
  <init>(Ljava/lang/String;I)V
    ALOAD 0
    ALOAD 1
    ILOAD 2
    INVOKESPECIAL java/lang/Enum.<init> (Ljava/lang/String;I)V
    RETURN

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
    LDC LMyEnum;.class
    ALOAD 0
    INVOKESTATIC java/lang/Enum.valueOf (Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    CHECKCAST MyEnum
    ARETURN
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
                            targetVar.isNull(
                                new StatementDef.Synchronized(
                                    aThis,
                                    StatementDef.multi(
                                        targetVar.assign(targetFieldAccess),
                                        targetVar.isNull(
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
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ASTORE 1
    ALOAD 1
    IFNONNULL L0
    TRYCATCHBLOCK L1 L2 L3 null
    TRYCATCHBLOCK L3 L4 L3 null
    ALOAD 0
    DUP
    ASTORE 2
    MONITORENTER
   L1
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ASTORE 1
    ALOAD 1
    IFNONNULL L5
    ALOAD 0
    ACONST_NULL
    CHECKCAST test/SomeTarget
    PUTFIELD test/MyClass.target : Ltest/SomeTarget;
    ALOAD 0
    ACONST_NULL
    CHECKCAST io/micronaut/context/BeanResolutionContext
    PUTFIELD test/MyClass.$beanResolutionContext : Lio/micronaut/context/BeanResolutionContext;
   L5
    ALOAD 2
    MONITOREXIT
   L2
    GOTO L6
   L3
    ASTORE 3
    ALOAD 2
    MONITOREXIT
   L4
    ALOAD 3
    ATHROW
   L6
   L0
    ALOAD 0
    GETFIELD test/MyClass.target : Ltest/SomeTarget;
    ARETURN
}
""", bytecode);


        Assertions.assertEquals("""
package test;

import io.micronaut.context.BeanResolutionContext;

class MyClass {
   private BeanResolutionContext $beanResolutionContext;
   private SomeTarget target;

   public Object interceptedTarget() {
      SomeTarget var1 = this.target;
      if (var1 == null) {
         synchronized(this) {
            var1 = this.target;
            if (var1 == null) {
               this.target = (SomeTarget)null;
               this.$beanResolutionContext = (BeanResolutionContext)null;
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
                        myFieldAccess.isNull(
                            ExpressionDef.constant(1).returning()
                        ),
                        myFieldAccess.isNonNull(
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
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Hello"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    TRYCATCHBLOCK L0 L1 L2 null
   L0
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
   L1
    GOTO L3
   L2
    ASTORE 4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "World"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ALOAD 4
    ATHROW
    GOTO L3
   L3
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   private final Object target;

   public Object swap(Object var1) {
      System.out.println("Hello");

      try {
         Object var2 = this.target;
         this.target = var1;
         System.out.println("World");
         return var2;
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
    ILOAD 1
    TABLESWITCH
      1: L0
      2: L1
      3: L0
      4: L0
      5: L0
      6: L0
      7: L0
      default: L2
   L0
    BIPUSH 123
    IRETURN
    GOTO L3
   L1
    BIPUSH 111
    IRETURN
    GOTO L3
   L2
    SIPUSH 444
    IRETURN
   L3
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   public int swap(int var1) {
      switch (var1) {
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
    ILOAD 1
    LOOKUPSWITCH
      1: L0
      20: L1
      30: L0
      40: L0
      50: L0
      60: L0
      70: L0
      default: L2
   L0
    BIPUSH 123
    IRETURN
    GOTO L3
   L1
    BIPUSH 111
    IRETURN
    GOTO L3
   L2
    SIPUSH 444
    IRETURN
   L3
}
""", bytecode);

        Assertions.assertEquals("""
package test;

class MyClass {
   public int swap(int var1) {
      switch (var1) {
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
