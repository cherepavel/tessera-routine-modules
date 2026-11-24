package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.domain;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.random.RandomGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class NumericRangeTest {
    @Test
    public void includesBothIntegerEndpoints() {
        NumericRange range = integers("-1", "1");
        Random random = new Random(42);
        Set<BigDecimal> values = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            BigDecimal value = range.generate(random);
            assertTrue(value.compareTo(BigDecimal.ONE.negate()) >= 0 && value.compareTo(BigDecimal.ONE) <= 0);
            values.add(value);
        }
        assertEquals(Set.of(BigDecimal.ONE.negate(), BigDecimal.ZERO, BigDecimal.ONE), values);
    }

    @Test
    public void supportsFullLongRangeAndOverflowingRangeSizes() {
        NumericRange full = integers(Long.toString(Long.MIN_VALUE), Long.toString(Long.MAX_VALUE));
        assertEquals(BigDecimal.valueOf(Long.MIN_VALUE), full.generate(() -> Long.MIN_VALUE));
        assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), full.generate(() -> Long.MAX_VALUE));
        NumericRange overflowing = integers("-1", Long.toString(Long.MAX_VALUE));
        RandomGenerator random = new RandomGenerator() {
            private int calls;
            @Override public long nextLong() { return calls++ == 0 ? Long.MIN_VALUE : Long.MAX_VALUE; }
        };
        assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), overflowing.generate(random));
        NumericRange nearMaximum = integers("9223372036854775806", "9223372036854775807");
        for (int i = 0; i < 20; i++) {
            assertTrue(nearMaximum.generate(new Random(i)).longValueExact() >= Long.MAX_VALUE - 1);
        }
    }

    @Test
    public void decimalEndpointsAreExactAndUpperExclusive() {
        NumericRange range = new NumericRange(new BigDecimal("0.1"), new BigDecimal("0.2"), NumericRange.ValueType.DECIMAL);
        RandomGenerator atMinimum = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public double nextDouble() { return 0; }
        };
        RandomGenerator nearMaximum = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public double nextDouble() { return Math.nextDown(1.0); }
        };
        assertEquals(0, range.generate(atMinimum).compareTo(new BigDecimal("0.1")));
        BigDecimal high = range.generate(nearMaximum);
        assertTrue(high.compareTo(new BigDecimal("0.1")) > 0);
        assertTrue(high.compareTo(new BigDecimal("0.2")) < 0);
    }

    @Test
    public void equalBoundsProduceConstantsWithoutConsumingRandomness() {
        RandomGenerator unused = () -> { throw new AssertionError("Constant range consumed randomness"); };
        assertEquals(0, integers("70", "70").generate(unused).compareTo(new BigDecimal("70")));
        NumericRange decimal = new NumericRange(new BigDecimal("-0.125"), new BigDecimal("-0.125"), NumericRange.ValueType.DECIMAL);
        assertEquals(new BigDecimal("-0.125"), decimal.generate(unused));
    }

    @Test
    public void rejectsInvertedFractionalAndOutOfRangeIntegerBounds() {
        assertThrows(IllegalArgumentException.class, () -> integers("2", "1"));
        assertThrows(IllegalArgumentException.class, () -> integers("0.1", "1"));
        assertThrows(IllegalArgumentException.class, () -> integers("0", "9223372036854775808"));
    }

    private static NumericRange integers(String minimum, String maximum) {
        return new NumericRange(new BigDecimal(minimum), new BigDecimal(maximum), NumericRange.ValueType.INTEGER);
    }
}
