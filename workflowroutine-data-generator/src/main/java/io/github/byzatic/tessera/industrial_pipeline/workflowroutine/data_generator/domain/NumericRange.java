package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * A numeric range: inclusive for integers, upper-exclusive for decimals unless constant.
 *
 * @param minimum inclusive lower bound
 * @param maximum upper bound; equal bounds produce a constant
 * @param type representation and endpoint semantics of the generated value
 */
public record NumericRange(BigDecimal minimum, BigDecimal maximum, ValueType type) {
    public NumericRange {
        Objects.requireNonNull(minimum, "minimum must not be null");
        Objects.requireNonNull(maximum, "maximum must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Min must be less than or equal to Max");
        }
        if (type == ValueType.INTEGER) {
            try {
                minimum.longValueExact();
                maximum.longValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("INTEGER bounds must be whole signed 64-bit numbers", e);
            }
        }
    }

    /**
     * Generates a value using only the supplied random source; equal bounds produce a constant.
     * @param random source of randomness
     * @return a value within this range
     */
    public BigDecimal generate(RandomGenerator random) {
        Objects.requireNonNull(random, "random must not be null");
        if (minimum.compareTo(maximum) == 0) {
            return minimum;
        }
        if (type == ValueType.DECIMAL) {
            return minimum.add(maximum.subtract(minimum).multiply(BigDecimal.valueOf(random.nextDouble())));
        }
        long lower = minimum.longValueExact();
        long upper = maximum.longValueExact();
        long size = upper - lower + 1;
        if (size > 0) {
            return BigDecimal.valueOf(lower + random.nextLong(size));
        }
        // The inclusive range contains more than Long.MAX_VALUE values; avoid bound overflow.
        long value;
        do {
            value = random.nextLong();
        } while (value < lower || value > upper);
        return BigDecimal.valueOf(value);
    }

    /** Supported numeric representations. */
    public enum ValueType {
        /** Whole signed 64-bit values, including both endpoints. */
        INTEGER,
        /** Decimal values with an exclusive upper endpoint, except constant ranges. */
        DECIMAL
    }
}
