package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An exact numeric comparison, independent of DSL and metric storage.
 * @param operator comparison to apply to the input value
 * @param threshold finite numeric threshold
 */
public record NumericCondition(Operator operator, BigDecimal threshold) {
    public NumericCondition {
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(threshold, "threshold must not be null");
    }

    /**
     * Compares by numeric value, ignoring differences in decimal scale.
     * @param value non-null numeric input
     * @return whether the comparison is satisfied
     */
    public boolean matches(BigDecimal value) {
        int comparison = value.compareTo(threshold);
        return switch (operator) {
            case GT -> comparison > 0;
            case GE -> comparison >= 0;
            case LT -> comparison < 0;
            case LE -> comparison <= 0;
            case EQ -> comparison == 0;
            case NE -> comparison != 0;
        };
    }

    /** @return the operator and threshold for inclusion in a processing reason */
    public String describe() {
        return operator.symbol() + " " + threshold.toPlainString();
    }

    /** Supported comparisons and their DSL parameter values. */
    public enum Operator {
        /** Greater than. */
        GT(">"),
        /** Greater than or equal. */
        GE(">="),
        /** Less than. */
        LT("<"),
        /** Less than or equal. */
        LE("<="),
        /** Numerically equal. */
        EQ("=="),
        /** Numerically unequal. */
        NE("!=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        /** @return the DSL spelling of this comparison */
        public String symbol() {
            return symbol;
        }

        /**
         * Resolves a comparison from its DSL spelling.
         * @param symbol one of the six supported comparison symbols
         * @return the corresponding comparison
         * @throws IllegalArgumentException if the symbol is unsupported
         */
        public static Operator fromSymbol(String symbol) {
            for (Operator operator : values()) {
                if (operator.symbol.equals(symbol)) {
                    return operator;
                }
            }
            throw new IllegalArgumentException("Unsupported comparison operator: " + symbol);
        }
    }
}
