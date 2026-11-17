package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.domain;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class StatusRulesTest {
    @Test
    public void evaluatesExactThresholdBoundariesAndPrioritizesAlarm() {
        StatusRules rules = new StatusRules(Optional.of(condition(">=", "70")), Optional.of(condition(">=", "90")));
        for (String value : List.of("-10", "0", "69.999999999999999999999")) {
            assertEquals(value, StatusRules.Severity.OK, rules.evaluate(new BigDecimal(value)).severity());
        }
        for (String value : List.of("70", "70.000000000000000000001", "89.999999999999999999999")) {
            assertEquals(value, StatusRules.Severity.WARNING, rules.evaluate(new BigDecimal(value)).severity());
        }
        for (String value : List.of("90", "90.000000000000000000001", "120")) {
            var evaluation = rules.evaluate(new BigDecimal(value));
            assertEquals(value, StatusRules.Severity.ALARM, evaluation.severity());
            assertEquals(rules.alarm(), evaluation.condition());
        }
    }

    @Test
    public void supportsAllComparisonOperatorsWithoutRoundingOrScaleDependentEquality() {
        List<String> operators = List.of(">", ">=", "<", "<=", "==", "!=");
        List<List<Boolean>> expected = List.of(
                List.of(false, false, true), List.of(false, true, true),
                List.of(true, false, false), List.of(true, true, false),
                List.of(false, true, false), List.of(true, false, true));
        List<String> values = List.of("9.99999999999999999999", "10.00", "10.00000000000000000001");
        for (int operator = 0; operator < operators.size(); operator++) {
            for (int value = 0; value < values.size(); value++) {
                assertEquals(operators.get(operator) + " " + values.get(value), expected.get(operator).get(value),
                        condition(operators.get(operator), "10.0").matches(new BigDecimal(values.get(value))));
            }
        }
    }

    @Test
    public void supportsWarningOnlyAlarmOnlyAndLowerBoundConditions() {
        StatusRules low = new StatusRules(Optional.of(condition("<=", "0")), Optional.of(condition("<", "-10")));
        assertEquals(StatusRules.Severity.WARNING, low.evaluate(BigDecimal.ZERO).severity());
        assertEquals(StatusRules.Severity.ALARM, low.evaluate(new BigDecimal("-10.01")).severity());
        StatusRules warningOnly = new StatusRules(Optional.of(condition("!=", "0")), Optional.empty());
        assertEquals(StatusRules.Severity.WARNING, warningOnly.evaluate(BigDecimal.ONE).severity());
        StatusRules alarmOnly = new StatusRules(Optional.empty(), Optional.of(condition("==", "0")));
        assertEquals(StatusRules.Severity.ALARM, alarmOnly.evaluate(BigDecimal.ZERO).severity());
        assertEquals(Optional.empty(), alarmOnly.evaluate(BigDecimal.ONE).condition());
    }

    @Test
    public void rejectsEmptyRulesAndUnsupportedOperators() {
        assertThrows(IllegalArgumentException.class, () -> new StatusRules(Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> NumericCondition.Operator.fromSymbol("contains"));
    }

    private static NumericCondition condition(String operator, String threshold) {
        return new NumericCondition(NumericCondition.Operator.fromSymbol(operator), new BigDecimal(threshold));
    }
}
