package com.lwe.core.service;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FormulaEvaluatorTest {

    private static final Set<String> ATTRS = Set.of("staerke", "geschick", "konstitution", "intelligenz",
        "weisheit", "charisma", "mut", "klugheit", "intuition", "koerperkraft");

    private double eval(String expr, Map<String, Integer> vars) {
        return FormulaEvaluator.eval(expr, vars, ATTRS);
    }

    @Test
    void einfacheAddition() {
        assertEquals(8.0, eval("3+5", Map.of()));
    }

    @Test
    void einfacheSubtraktion() {
        assertEquals(3.0, eval("10-7", Map.of()));
    }

    @Test
    void multiplikation() {
        assertEquals(15.0, eval("3*5", Map.of()));
    }

    @Test
    void division() {
        assertEquals(5.0, eval("10/2", Map.of()));
    }

    @Test
    void operatorPrecedence() {
        assertEquals(13.0, eval("3+5*2", Map.of()));
    }

    @Test
    void klammern() {
        assertEquals(16.0, eval("(3+5)*2", Map.of()));
    }

    @Test
    void variableBare() {
        assertEquals(15.0, eval("staerke", Map.of("staerke", 15)));
    }

    @Test
    void variableAtSyntax() {
        assertEquals(15.0, eval("@{staerke}", Map.of("staerke", 15)));
    }

    @Test
    void variableInExpression() {
        assertEquals(18.5, eval("(@{konstitution}+@{koerperkraft})/2+5",
            Map.of("konstitution", 14, "koerperkraft", 13)));
    }

    @Test
    void dsaLePFormel() {
        // (KO+KK)/2+5 mit KO=14, KK=13 → (14+13)/2+5 = 27/2+5 = 13.5+5 = 18.5
        assertEquals(18.5, eval("(@{konstitution}+@{koerperkraft})/2+5",
            Map.of("konstitution", 14, "koerperkraft", 13)));
    }

    @Test
    void ddModifier() {
        assertEquals(2.5, eval("(staerke-10)/2", Map.of("staerke", 15)));
    }

    @Test
    void ddModifierFloored() {
        assertEquals(2.0, eval("floor((staerke-10)/2)", Map.of("staerke", 15)));
    }

    @Test
    void negativeNumbers() {
        assertEquals(-3.0, eval("-3", Map.of()));
    }

    @Test
    void minFunction() {
        assertEquals(5.0, eval("min(5,10)", Map.of()));
    }

    @Test
    void maxFunction() {
        assertEquals(10.0, eval("max(5,10)", Map.of()));
    }

    @Test
    void floorFunction() {
        assertEquals(5.0, eval("floor(5.7)", Map.of()));
    }

    @Test
    void nestedFunctions() {
        assertEquals(8.0, eval("max(min(5,10),8)", Map.of()));
    }

    @Test
    void unknownVariableThrows() {
        assertThrows(FormulaEvaluator.EvaluationException.class,
            () -> eval("@unbekannt", Map.of()));
    }

    @Test
    void divisionByZeroThrows() {
        assertThrows(FormulaEvaluator.EvaluationException.class,
            () -> eval("5/0", Map.of()));
    }

    @Test
    void missingClosingParenThrows() {
        assertThrows(FormulaEvaluator.EvaluationException.class,
            () -> eval("(3+5*2", Map.of()));
    }

    @Test
    void emptyExpressionThrows() {
        assertThrows(FormulaEvaluator.EvaluationException.class,
            () -> eval("", Map.of()));
    }

    @Test
    void onlyVariable() {
        assertEquals(14.0, eval("@{konstitution}", Map.of("konstitution", 14)));
    }

    @Test
    void spacesAroundOperators() {
        // Audit P28: freie Eingabefelder laden zu "mut + klugheit" ein.
        assertEquals(26.0, eval("mut + klugheit", Map.of("mut", 14, "klugheit", 12)));
        assertEquals(7.0, eval("( mut + klugheit ) / 2", Map.of("mut", 6, "klugheit", 8)));
        assertEquals(14.0, eval("mut*2 - 14", Map.of("mut", 14)));
    }

    @Test
    void spacesInFunctionsAndArgs() {
        assertEquals(6.0, eval("min( mut , klugheit )", Map.of("mut", 14, "klugheit", 6)));
        assertEquals(14.0, eval("max( mut , klugheit )", Map.of("mut", 14, "klugheit", 6)));
        assertEquals(3.0, eval("floor( mut / 4 )", Map.of("mut", 14)));
    }
}
