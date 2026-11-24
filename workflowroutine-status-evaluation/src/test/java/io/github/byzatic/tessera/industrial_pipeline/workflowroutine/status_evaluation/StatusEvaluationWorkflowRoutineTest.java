package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.dto.DataItem;
import io.github.byzatic.tessera.storageapi.dto.StorageItem;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StatusEvaluationWorkflowRoutineTest {
    private static final String SCRIPT = """
            GET FROM SELF STORAGE MEASUREMENTS BY DATA ID temperature AS RAW;
            PROCESS FUNCTION EvaluateStatus("DataId=RAW", "WarningThreshold=70", "AlarmThreshold=90") RETURN STATUS;
            PUT DATA STATUS TO STORAGE STATUS MODIFIER global BY DATA ID status;
            """;

    @Test
    public void executesDslFileAndPublishesToGlobalStorage() throws Exception {
        Map<RoutineTestApi.Key, StorageItem> storage = measurements("90");
        Path file = Files.createTempFile("status-evaluation-", ".mcg3dsl");
        try {
            Files.writeString(file, SCRIPT);
            HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
            var routine = new StatusEvaluationWorkflowRoutineFactory().create(
                    new RoutineTestApi(storage, "sensor", "MCg3-WorkflowRoutine-DSL-File", file.toString()), health);
            routine.run();
            assertEquals("1.0.0", routine.getWorkflowRoutineVersion().toString());
            assertEquals(HealthFlagState.COMPLETE, health.getHealthFlagState());
            var result = storage.get(new RoutineTestApi.Key("GLOBAL", "STATUS", "status"));
            assertEquals("2", ((DataItem) result.getDataValue()).getMetricValue());
            assertEquals(StorageItem.ScopeType.GLOBAL, result.getScope());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void failsMissingMalformedAndInvalidInputsWithoutPublishingStatus() {
        for (String value : List.of("NaN", "Infinity", "bad-value")) {
            assertFailure(measurements(value), SCRIPT);
        }
        assertFailure(new HashMap<>(), SCRIPT);
        assertFailure(measurements("90"), SCRIPT.replace("AlarmThreshold=90", "AlarmThreshold=bad"));
        assertFailure(measurements("90"), SCRIPT.replace("EvaluateStatus", "Unknown"));
    }

    @Test
    public void clearsLocalResultsAfterFailureBeforeNextRun() {
        Map<RoutineTestApi.Key, StorageItem> storage = measurements("90");
        HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
        var routine = new StatusEvaluationWorkflowRoutineFactory().create(new RoutineTestApi(storage, "sensor", SCRIPT), health);
        routine.run();
        storage.clear();
        assertThrows(RuntimeException.class, routine::run);
        assertEquals(HealthFlagState.FATAL, health.getHealthFlagState());
        assertTrue(storage.isEmpty());
    }

    private static void assertFailure(Map<RoutineTestApi.Key, StorageItem> storage, String script) {
        HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
        var routine = new StatusEvaluationWorkflowRoutineFactory().create(new RoutineTestApi(storage, "sensor", script), health);
        assertThrows(RuntimeException.class, routine::run);
        assertEquals(HealthFlagState.FATAL, health.getHealthFlagState());
        assertTrue(storage.keySet().stream().noneMatch(key -> key.storage().equals("STATUS")));
    }

    private static Map<RoutineTestApi.Key, StorageItem> measurements(String value) {
        Map<RoutineTestApi.Key, StorageItem> storage = new HashMap<>();
        StorageItem item = StorageItem.newBuilder().setScope(StorageItem.ScopeType.LOCAL)
                .setStorageId("MEASUREMENTS").setDataId("temperature")
                .setDataValue(DataItem.newBuilder().setMetricName("temperature").setMetricValue(value)
                        .setMetricCreationTime(Instant.EPOCH).setMetricLabels(List.of()).build()).build();
        storage.put(new RoutineTestApi.Key("sensor", "MEASUREMENTS", "temperature"), item);
        return storage;
    }
}
