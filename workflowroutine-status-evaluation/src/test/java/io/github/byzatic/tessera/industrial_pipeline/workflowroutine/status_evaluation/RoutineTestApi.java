package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

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

import java.util.List;
import java.util.Map;

/** In-memory Storage API shared by separately constructed routine instances in pipeline tests. */
final class RoutineTestApi implements MCg3WorkflowRoutineApiInterface, StorageApiInterface {
    private final Map<Key, StorageItem> storage;
    private final String node;
    private final List<ConfigurationParameter> parameters;

    RoutineTestApi(Map<Key, StorageItem> storage, String node, String script) {
        this(storage, node, "MCg3-WorkflowRoutine-DSL", script);
    }

    RoutineTestApi(Map<Key, StorageItem> storage, String node, String key, String script) {
        this.storage = storage;
        this.node = node;
        parameters = List.of(ConfigurationParameter.newBuilder().parameterKey(key).parameterValue(script).build());
    }

    @Override public StorageApiInterface getStorageApi() { return this; }
    @Override public List<ConfigurationParameter> getConfigurationParameters() { return parameters; }

    @Override
    public void putStorageObject(StorageItem item) {
        storage.put(key(item), item);
    }

    @Override
    public StorageItem getStorageObject(StorageItem item) {
        StorageItem result = storage.get(key(item));
        if (result == null) {
            throw new IllegalArgumentException("Missing storage item: " + key(item));
        }
        return result;
    }

    DataItem local(String storageId, String dataId) {
        return (DataItem) storage.get(new Key(node, storageId, dataId)).getDataValue();
    }

    private Key key(StorageItem item) {
        String owner = switch (item.getScope()) {
            case GLOBAL -> "GLOBAL";
            case DOWNSTREAM -> item.getDownstreamName();
            case LOCAL -> node;
        };
        return new Key(owner, item.getStorageId(), item.getDataId());
    }

    @Override public Boolean isDataByIdExists(StorageItem item) { return storage.containsKey(key(item)); }
    @Override public List<StorageItem> listStorageObjects(StorageItem item) { throw new UnsupportedOperationException(); }

    @Override
    public ExecutionContextInterface getExecutionContext() {
        return new ExecutionContextInterface() {
            @Override public PipelineDescriptionInterface getPipelineDescription() { throw new UnsupportedOperationException(); }
            @Override public NodeDescriptionInterface getNodeDescription() { throw new UnsupportedOperationException(); }
            @Override public List<StorageDescriptionInterface> getGlobalStoragesDescription() { return List.of(); }
            @Override public PipelineExecutionInfoInterface getPipelineExecutionInfo() { return () -> () -> "root." + node; }
            @Override public MdcContextInterface getMdcContext() {
                return new MdcContextInterface() {
                    @Override public void apply() { }
                    @Override public void clear() { }
                    @Override public AutoCloseable use() { return () -> { }; }
                };
            }
        };
    }

    record Key(String node, String storage, String dataId) { }
}
