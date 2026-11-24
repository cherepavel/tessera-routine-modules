package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.application;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.exception.RoutineExecutionException;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.FunctionArguments;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.StorageLocation;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.repository.MetricStorage;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.RoutineWorkspace;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricLabel;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricSample;
import org.junit.Test;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DataGeneratorFunctionHandlerTest {
    private final RoutineWorkspace workspace = new RoutineWorkspace(new UnsupportedMetricStorage(), () -> "root.source");

    @Test
    public void generatesAllThreeStatusesWithTimestampsAndReasons() throws Exception {
        DataGeneratorFunctionHandler handler = new DataGeneratorFunctionHandler(new Random(42));
        Set<String> statuses = new HashSet<>();
        Instant before = Instant.now();

        for (int i = 0; i < 100; i++) {
            MetricSample metric = handler.execute("GenerateData", FunctionArguments.parse(List.of()), "STATUS", workspace);

            statuses.add(metric.value());
            assertEquals("status", metric.name());
            assertTrue(Set.of("0", "1", "2").contains(metric.value()));
            assertFalse(metric.createdAt().isBefore(before));
            assertFalse(metric.createdAt().isAfter(Instant.now()));
            assertEquals(
                    metric.value().equals("0") ? List.of() : List.of(new MetricLabel(
                            "reason", "root.source => Generated test status: " + metric.value(), "="
                    )),
                    metric.labels()
            );
        }

        assertEquals(Set.of("0", "1", "2"), statuses);
    }

    @Test
    public void rejectsUnknownFunction() {
        DataGeneratorFunctionHandler handler = new DataGeneratorFunctionHandler(new Random(42));

        RoutineExecutionException failure = assertThrows(RoutineExecutionException.class, () -> handler.execute(
                "Unknown", FunctionArguments.parse(List.of()), "STATUS", workspace
        ));

        assertTrue(failure.getMessage().contains("Unknown"));
    }

    @Test
    public void generatesRawNumbersWithNamesLabelsAndNoStatusReason() throws Exception {
        DataGeneratorFunctionHandler handler = new DataGeneratorFunctionHandler(new Random(42));
        FunctionArguments arguments = FunctionArguments.parse(List.of(
                "Min=-12.5", "Max=30.75", "MetricName=temperature", "PromLabel_site=north"));
        Instant before = Instant.now();
        for (int i = 0; i < 100; i++) {
            MetricSample sample = handler.execute("GenerateNumber", arguments, "RAW", workspace);
            BigDecimal value = new BigDecimal(sample.value());
            assertTrue(value.compareTo(new BigDecimal("-12.5")) >= 0);
            assertTrue(value.compareTo(new BigDecimal("30.75")) < 0);
            assertEquals("temperature", sample.name());
            assertEquals(List.of(new MetricLabel("site", "north", "=")), sample.labels());
            assertFalse(sample.createdAt().isBefore(before));
            assertFalse(sample.createdAt().isAfter(Instant.now()));
        }
    }

    @Test
    public void reproducesASequenceAndSupportsIntegersAndDefaultMetricNames() throws Exception {
        DataGeneratorFunctionHandler first = new DataGeneratorFunctionHandler(new Random(42));
        DataGeneratorFunctionHandler second = new DataGeneratorFunctionHandler(new Random(42));
        FunctionArguments arguments = FunctionArguments.parse(List.of("Min=-10", "Max=10", "Type=INTEGER"));
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            MetricSample sample = first.execute("GenerateNumber", arguments, "NUMBER", workspace);
            assertEquals(sample.value(), second.execute("GenerateNumber", arguments, "NUMBER", workspace).value());
            assertEquals("number", sample.name());
            long value = new BigDecimal(sample.value()).longValueExact();
            assertTrue(value >= -10 && value <= 10);
            values.add(sample.value());
        }
        assertTrue(new HashSet<>(values).size() > 1);
    }

    @Test
    public void rejectsInvalidNumericConfiguration() {
        DataGeneratorFunctionHandler handler = new DataGeneratorFunctionHandler(new Random(42));
        List<List<String>> invalid = List.of(
                List.of("Max=1"), List.of("Min=0"), List.of("Min=2", "Max=1"),
                List.of("Min=NaN", "Max=1"), List.of("Min=0", "Max=Infinity"),
                List.of("Min=0", "Max=1", "Type=FLOAT"),
                List.of("Min=0.5", "Max=1", "Type=INTEGER"),
                List.of("Min=0", "Max=9223372036854775808", "Type=INTEGER"),
                List.of("Min=0", "Max=1", "MetricName="),
                List.of("Min=0", "Min=1", "Max=2"));
        for (List<String> arguments : invalid) {
            assertThrows(arguments.toString(), IllegalArgumentException.class, () -> handler.execute(
                    "GenerateNumber", FunctionArguments.parse(arguments), "NUMBER", workspace));
        }
    }

    private static final class UnsupportedMetricStorage implements MetricStorage {
        @Override
        public MetricSample load(StorageLocation location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(StorageLocation location, MetricSample metric) {
            throw new UnsupportedOperationException();
        }
    }
}
