package io.micronaut.sourcegen.javapoet.write;

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.ExpressionDef.Cast;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExpressionWriteTest extends AbstractWriteTest {

    private static final ClassTypeDef STRING = ClassTypeDef.STRING;

    @Test
    public void equalsExpressions() throws IOException {
        ExpressionDef exp1 = ExpressionDef.constant(
            ClassElement.of(String.class), STRING, "hello"
        );
        ExpressionDef exp2 = ExpressionDef.constant(
            ClassElement.of(String.class), STRING, "world"
        );
        String equalsReferentially = writeMethodWithExpression(exp1.equalsReferentially(exp2));

        assertEquals("\"hello\" == \"world\"", equalsReferentially);

        String notEqualsReferentially = writeMethodWithExpression(exp1.notEqualsReferentially(exp2));

        assertEquals("\"hello\" != \"world\"", notEqualsReferentially);

        String equalsStructurally = writeMethodWithExpression(exp1.equalsStructurally(exp2));

        assertEquals("\"hello\".equals(\"world\")", equalsStructurally);

        String notEqualsStructurally = writeMethodWithExpression(exp1.notEqualsStructurally(exp2));

        assertEquals("(!\"hello\".equals(\"world\"))", notEqualsStructurally);
    }

    @Test
    public void returnConstantExpression() throws IOException {
        ExpressionDef helloString = ExpressionDef.constant(
            ClassElement.of(String.class), STRING, "hello"
        );
        String result = writeMethodWithExpression(helloString);

        assertEquals("\"hello\"", result);
    }

    @Test
    public void returnStaticInvoke() throws IOException {
        ExpressionDef two = ExpressionDef.constant(
            ClassElement.of(int.class), TypeDef.Primitive.INT, "2"
        );
        ExpressionDef valueOfTwo = STRING.invokeStatic(
            "valueOf", STRING, two
        );
        String result = writeMethodWithExpression(valueOfTwo);

        assertEquals("String.valueOf(2)", result);
    }

    @Test
    public void returnInvoke() throws IOException {
        ExpressionDef helloString = ExpressionDef.constant(
            ClassElement.of(String.class), STRING, "hello"
        );
        ExpressionDef equals = new VariableDef.This().invoke("equals", TypeDef.Primitive.BOOLEAN, helloString);
        String result = writeMethodWithExpression(equals);

        assertEquals("this.equals(\"hello\")", result);
    }

    @Test
    public void returnConstantStringArray() throws IOException {
        ExpressionDef stringArray = new VariableDef.Constant(TypeDef.array(ClassTypeDef.of(String.class)),
            new String[] {"hello", "world"});
        String result = writeMethodWithExpression(stringArray);

        assertEquals("new String[] {\"hello\", \"world\"}", result);
    }

    @Test
    public void returnConstantIntegerArray() throws IOException {
        ExpressionDef integerArray = new VariableDef.Constant(TypeDef.array(ClassTypeDef.of(Integer.class)),
            new Integer[] {1, 2});
        String result = writeMethodWithExpression(integerArray);

        assertEquals("new Integer[] {1, 2}", result);
    }

    @Test
    public void returnConstantIntArray() throws IOException {
        ExpressionDef integerArray = new VariableDef.Constant(TypeDef.array(TypeDef.primitive(Integer.TYPE)),
            new int[] {1, 2});
        String result = writeMethodWithExpression(integerArray);

        assertEquals("new int[] {1, 2}", result);
    }

    @Test
    public void returnCastedValue() throws IOException {
        ExpressionDef castedExpression = ExpressionDef
            .constant(ClassElement.of(Double.TYPE), TypeDef.Primitive.DOUBLE, 10.5)
            .cast(TypeDef.Primitive.FLOAT);
        String result = writeMethodWithExpression(castedExpression);

        assertEquals("(float) (10.5d)", result);
    }

    @Test
    public void returnCastedValue2() throws IOException {
        ExpressionDef castedExpression = new Cast(
            TypeDef.of(Object.class),
            ExpressionDef.constant(ClassElement.of(String.class), TypeDef.of(String.class), "hello")
        );
        String result = writeMethodWithExpression(castedExpression);

        assertEquals("(Object) (\"hello\")", result);
    }

    @Test
    public void returnCastedVariable() throws IOException {
        ExpressionDef castedExpression = new Cast(
            TypeDef.of(Integer.class),
            new VariableDef.Local("field", TypeDef.of(Object.class))
        );
        String result = writeMethodWithExpression(castedExpression);

        assertEquals("(Integer) field", result);
    }

    @Test
    public void returnAndCondition() throws IOException {
        ExpressionDef andExpression = new ExpressionDef.And(
            ExpressionDef.trueValue().isTrue(),
            new VariableDef.Local("field", TypeDef.Primitive.BOOLEAN).isTrue()
        );
        String result = writeMethodWithExpression(andExpression);

        assertEquals("true && field", result);
    }

    @Test
    public void returnAndConditionFalse() throws IOException {
        ExpressionDef andExpression = new ExpressionDef.And(
            ExpressionDef.trueValue().isTrue(),
            new VariableDef.Local("field", TypeDef.Primitive.BOOLEAN).isFalse()
        );
        String result = writeMethodWithExpression(andExpression);

        assertEquals("true && !field", result);
    }

    @Test
    public void returnAndConditionWithParentheses() throws IOException {
        ExpressionDef andExpression = new ExpressionDef.And(
            ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue()),
            ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue())
        );
        String result = writeMethodWithExpression(andExpression);

        assertEquals("(true || false) && (true || false)", result);
    }

    @Test
    public void returnOrCondition() throws IOException {
        ExpressionDef orExpression = new ExpressionDef.Or(
            ExpressionDef.trueValue().isTrue(),
            new VariableDef.Local("field", TypeDef.Primitive.BOOLEAN).isTrue()
        );
        String result = writeMethodWithExpression(orExpression);

        assertEquals("(true || field)", result);
    }

    @Test
    public void returnOrConditionWithParentheses() throws IOException {
        ExpressionDef orExpression = new ExpressionDef.Or(
            ExpressionDef.trueValue().isTrue().and(ExpressionDef.falseValue().isTrue()),
            ExpressionDef.trueValue().isTrue().or(ExpressionDef.falseValue().isTrue())
        );
        String result = writeMethodWithExpression(orExpression);

        assertEquals("(true && false || (true || false))", result);
    }

    @Test
    public void returnPrimitiveInitialization() throws IOException {
        ExpressionDef intExpression = ExpressionDef.constant(0);
        String result = writeMethodWithExpression(intExpression);

        assertEquals("0", result);
    }

    @Test
    public void returnPrimitiveInitialization2() throws IOException {
        ExpressionDef intExpression = TypeDef.Primitive.INT.constant(0);
        String result = writeMethodWithExpression(intExpression);

        assertEquals("0", result);
    }
}
