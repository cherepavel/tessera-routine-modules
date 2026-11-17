package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.storageapi.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;

/** Registers status evaluation with the Tessera routine loader. */
@AutoService(WorkflowRoutineFactoryInterface.class)
public final class StatusEvaluationWorkflowRoutineFactory implements WorkflowRoutineFactoryInterface {
    @Override
    public StatusEvaluationWorkflowRoutine create(MCg3WorkflowRoutineApiInterface api, HealthFlagProxy health) {
        try {
            return new StatusEvaluationWorkflowRoutine(api, health);
        } catch (MCg3ApiOperationIncompleteException e) {
            throw new IllegalStateException("Failed to create status evaluation routine", e);
        }
    }
}
