package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates alarm before warning; a value matching neither condition is OK.
 * @param warning optional warning condition
 * @param alarm optional alarm condition; at least one condition must be present
 */
public record StatusRules(Optional<NumericCondition> warning, Optional<NumericCondition> alarm) {
    public StatusRules {
        Objects.requireNonNull(warning, "warning must not be null");
        Objects.requireNonNull(alarm, "alarm must not be null");
        if (warning.isEmpty() && alarm.isEmpty()) {
            throw new IllegalArgumentException("At least one WarningThreshold or AlarmThreshold is required");
        }
    }

    /**
     * Selects the highest matching severity.
     * @param value non-null numeric input
     * @return the severity and its matching condition, or OK with no condition
     */
    public Evaluation evaluate(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        if (alarm.filter(condition -> condition.matches(value)).isPresent()) {
            return new Evaluation(Severity.ALARM, alarm);
        }
        if (warning.filter(condition -> condition.matches(value)).isPresent()) {
            return new Evaluation(Severity.WARNING, warning);
        }
        return new Evaluation(Severity.OK, Optional.empty());
    }

    /** Status levels compatible with ProcessStatus. */
    public enum Severity {
        /** Neither condition matched. */
        OK("0"),
        /** Only the warning condition matched. */
        WARNING("1"),
        /** The alarm condition matched, regardless of warning. */
        ALARM("2");

        private final String value;

        Severity(String value) {
            this.value = value;
        }

        /** @return the status value used in metric storage */
        public String value() {
            return value;
        }
    }

    /**
     * An evaluated status and the condition explaining a nonzero result.
     * @param severity selected status level
     * @param condition selected condition; empty for OK
     */
    public record Evaluation(Severity severity, Optional<NumericCondition> condition) {
    }
}
