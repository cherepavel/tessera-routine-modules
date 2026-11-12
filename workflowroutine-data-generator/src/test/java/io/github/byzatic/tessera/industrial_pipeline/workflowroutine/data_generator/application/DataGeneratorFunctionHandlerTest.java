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
