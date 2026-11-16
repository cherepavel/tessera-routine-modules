package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.storageapi.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;

/** Registers the generator with the Tessera routine loader. */
@AutoService(WorkflowRoutineFactoryInterface.class)
public final class DataGeneratorWorkflowRoutineFactory implements WorkflowRoutineFactoryInterface {
    @Override
    public DataGeneratorWorkflowRoutine create(
            MCg3WorkflowRoutineApiInterface api,
            HealthFlagProxy healthFlagProxy
    ) {
        try {
            return new DataGeneratorWorkflowRoutine(api, healthFlagProxy);
        } catch (MCg3ApiOperationIncompleteException e) {
            throw new IllegalStateException("Failed to create data generator routine", e);
        }
    }
}
