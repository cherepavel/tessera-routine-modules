package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.application;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.exception.RoutineExecutionException;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.FunctionArguments;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.StorageLocation;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.repository.MetricStorage;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.RoutineWorkspace;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricLabel;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricSample;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.service.MetricLabels;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StatusEvaluationFunctionHandlerTest {
    private final StatusEvaluationFunctionHandler handler = new StatusEvaluationFunctionHandler();

    @Test
    public void publishesNewStatusWithSourceValueRuleAndGraphPathInReason() throws Exception {
        RoutineWorkspace workspace = workspace("90.00");
        Instant before = Instant.now();
        MetricSample status = handler.execute("EvaluateStatus", arguments(
                "DataId=RAW", "WarningThreshold=70", "AlarmThreshold=90", "MetricName=temperature_status",
                "PromLabel_site=south", "PromLabel_reason=must-not-override-rule"), "STATUS", workspace);

        assertEquals("2", status.value());
        assertEquals("temperature_status", status.name());
        assertEquals("south", MetricLabels.find(status.labels(), "site").orElseThrow().value());
        assertEquals("celsius", MetricLabels.find(status.labels(), "unit").orElseThrow().value());
        assertEquals("root.sensor => temperature [DataId=RAW]=90.00 matches ALARM condition >= 90",
                MetricLabels.find(status.labels(), "reason").orElseThrow().value());
        assertFalse(status.createdAt().isBefore(before));
        assertFalse(status.createdAt().isAfter(Instant.now()));
        assertEquals("90.00", workspace.metric("RAW").value());
        assertEquals("stale reason", MetricLabels.find(workspace.metric("RAW").labels(), "reason").orElseThrow().value());
    }

    @Test
    public void clearsStaleReasonsForOkAndDefaultsMetricNameToResultId() throws Exception {
        MetricSample status = handler.execute("EvaluateStatus", arguments(
                "DataId=RAW", "WarningThreshold=70", "AlarmThreshold=90", "PromLabel_reason=stale"),
                "STATUS", workspace("69.999"));
        assertEquals("0", status.value());
        assertEquals("status", status.name());
        assertTrue(MetricLabels.find(status.labels(), "reason").isEmpty());
        assertEquals("north", MetricLabels.find(status.labels(), "site").orElseThrow().value());
    }

    @Test
    public void supportsExplicitOperatorsAndExactDecimalComparisons() throws Exception {
        MetricSample status = handler.execute("EvaluateStatus", arguments(
                "DataId=RAW", "WarningOperator=<", "WarningThreshold=0.10000000000000000000000000001"),
                "STATUS", workspace("0.1"));
        assertEquals("1", status.value());
    }

    @Test
    public void rejectsMissingMetricAndInvalidOrAmbiguousConfiguration() {
        List<List<String>> invalid = List.of(
                List.of("DataId=RAW"),
                List.of("WarningThreshold=70"),
                List.of("DataId=MISSING", "WarningThreshold=70"),
                List.of("DataId=RAW", "DataId=OTHER", "WarningThreshold=70"),
                List.of("DataId=RAW", "WarningOperator=>", "AlarmThreshold=90"),
                List.of("DataId=RAW", "WarningThreshold=NaN"),
                List.of("DataId=RAW", "WarningThreshold=Infinity"),
                List.of("DataId=RAW", "WarningThreshold="),
                List.of("DataId=RAW", "WarningThreshold=70", "WarningOperator=invalid"),
                List.of("DataId=RAW", "WarningThreshold=70", "WarningThreshold=80"),
                List.of("DataId=RAW", "WarningThreshold=70", "MetricName="));
        for (List<String> arguments : invalid) {
            assertThrows(arguments.toString(), IllegalArgumentException.class, () -> handler.execute(
                    "EvaluateStatus", FunctionArguments.parse(arguments), "STATUS", workspace("90")));
        }
    }

    @Test
    public void rejectsNonNumericAndNonFiniteMeasurements() {
        for (String value : List.of("", "NaN", "Infinity", "-Infinity", "not-a-number")) {
            var failure = assertThrows(IllegalArgumentException.class, () -> handler.execute("EvaluateStatus",
                    arguments("DataId=RAW", "AlarmThreshold=90"), "STATUS", workspace(value)));
            assertTrue(failure.getMessage(), failure.getMessage().contains("RAW"));
        }
    }

    @Test
    public void rejectsUnknownFunction() {
        assertThrows(RoutineExecutionException.class, () -> handler.execute("Unknown", arguments(), "STATUS", workspace("0")));
    }

    private static FunctionArguments arguments(String... values) {
        return FunctionArguments.parse(List.of(values));
    }

    private static RoutineWorkspace workspace(String value) {
        RoutineWorkspace workspace = new RoutineWorkspace(new MetricStorage() {
            @Override public MetricSample load(StorageLocation location) { throw new UnsupportedOperationException(); }
            @Override public void save(StorageLocation location, MetricSample metric) { throw new UnsupportedOperationException(); }
        }, () -> "root.sensor");
        workspace.save("RAW", new MetricSample("temperature", value, Instant.EPOCH, List.of(
                new MetricLabel("site", "north", "="), new MetricLabel("unit", "celsius", "="),
                new MetricLabel("reason", "stale reason", "="))));
        return workspace;
    }
}
