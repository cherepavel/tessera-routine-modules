package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.dto.DataItem;
import io.github.byzatic.tessera.storageapi.dto.StorageItem;
import io.github.byzatic.tessera.storageapi.storageapi.StorageApiInterface;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.configuration.ConfigurationParameter;
import io.github.byzatic.tessera.workflowroutine.execution_context.ExecutionContextInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.NodeDescriptionInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.PipelineDescriptionInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.PipelineExecutionInfoInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.StorageDescriptionInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DataGeneratorWorkflowRoutineTest {
    private static final String SCRIPT = """
            PROCESS FUNCTION GenerateData RETURN STATUS;
            PUT DATA STATUS TO STORAGE StatusStorage BY DATA ID Status;
            """;

    @Test
    public void executesInlineDslAndPublishesCompatibleDataItemsOnEveryRun() {
        TestApi api = new TestApi("MCg3-WorkflowRoutine-DSL", SCRIPT);
        HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
        DataGeneratorWorkflowRoutine routine = new DataGeneratorWorkflowRoutineFactory().create(api, health);

        routine.run();
        routine.run();

        assertEquals("DataGeneratorWorkflowRoutine", routine.getName());
        assertEquals(HealthFlagState.COMPLETE, health.getHealthFlagState());
        assertEquals(2, api.writes.size());
        for (StorageItem item : api.writes) {
            assertEquals(StorageItem.ScopeType.LOCAL, item.getScope());
            assertEquals("StatusStorage", item.getStorageId());
            assertEquals("Status", item.getDataId());
            DataItem metric = (DataItem) item.getDataValue();
            assertEquals("status", metric.getMetricName());
            assertTrue(Set.of("0", "1", "2").contains(metric.getMetricValue()));
            assertNotNull(metric.getMetricCreationTime());
        }
    }

    @Test
    public void executesDslFileAndSupportsGlobalStorage() throws Exception {
        Path scriptFile = Files.createTempFile("data-generator-", ".mcg3dsl");
        try {
            Files.writeString(scriptFile, SCRIPT.replace("TO STORAGE StatusStorage", "TO STORAGE StatusStorage MODIFIER global"));
            TestApi api = new TestApi("MCg3-WorkflowRoutine-DSL-File", scriptFile.toString());
            HealthFlagProxy health = HealthFlagProxy.newBuilder().build();

            new DataGeneratorWorkflowRoutineFactory().create(api, health).run();

            assertEquals(HealthFlagState.COMPLETE, health.getHealthFlagState());
            assertEquals(1, api.writes.size());
            assertEquals(StorageItem.ScopeType.GLOBAL, api.writes.get(0).getScope());
        } finally {
            Files.deleteIfExists(scriptFile);
        }
    }

    @Test
    public void marksFailedExecutionAsFatal() {
        TestApi api = new TestApi("MCg3-WorkflowRoutine-DSL", "PROCESS FUNCTION Unknown RETURN STATUS;");
        HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
        DataGeneratorWorkflowRoutine routine = new DataGeneratorWorkflowRoutineFactory().create(api, health);

        assertThrows(RuntimeException.class, routine::run);

        assertEquals(HealthFlagState.FATAL, health.getHealthFlagState());
        assertTrue(api.writes.isEmpty());
    }

    private static final class TestApi implements MCg3WorkflowRoutineApiInterface, StorageApiInterface {
        private final List<ConfigurationParameter> parameters;
        private final List<StorageItem> writes = new ArrayList<>();

        private TestApi(String key, String value) {
            parameters = List.of(ConfigurationParameter.newBuilder().parameterKey(key).parameterValue(value).build());
        }

        @Override
        public StorageApiInterface getStorageApi() {
            return this;
        }

        @Override
        public ExecutionContextInterface getExecutionContext() {
            return new TestExecutionContext();
        }

        @Override
        public List<ConfigurationParameter> getConfigurationParameters() {
            return parameters;
        }

        @Override
        public void putStorageObject(StorageItem item) {
            writes.add(item);
        }

        @Override
        public StorageItem getStorageObject(StorageItem item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean isDataByIdExists(StorageItem item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<StorageItem> listStorageObjects(StorageItem item) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestExecutionContext implements ExecutionContextInterface {
        @Override
        public PipelineDescriptionInterface getPipelineDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeDescriptionInterface getNodeDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<StorageDescriptionInterface> getGlobalStoragesDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PipelineExecutionInfoInterface getPipelineExecutionInfo() {
            return () -> () -> "root.source";
        }

        @Override
        public MdcContextInterface getMdcContext() {
            return new MdcContextInterface() {
                @Override
                public void apply() {
                }

                @Override
                public void clear() {
                }

                @Override
                public AutoCloseable use() {
                    return () -> { };
                }
            };
        }
    }
}
