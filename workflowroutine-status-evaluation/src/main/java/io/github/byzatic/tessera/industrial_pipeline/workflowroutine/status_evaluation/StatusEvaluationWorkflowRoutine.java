package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

import com.github.zafarkhaja.semver.Version;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.usecase.RunRoutineUseCase;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.composition.TesseraRoutineApplicationFactory;
import io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.application.StatusEvaluationFunctionHandler;
import io.github.byzatic.tessera.storageapi.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.AbstractWorkflowRoutine;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Composition root for numeric status evaluation. */
public final class StatusEvaluationWorkflowRoutine extends AbstractWorkflowRoutine {
    private static final Logger logger = LoggerFactory.getLogger(StatusEvaluationWorkflowRoutine.class);
    private final RunRoutineUseCase runRoutine;

    public StatusEvaluationWorkflowRoutine(MCg3WorkflowRoutineApiInterface api, HealthFlagProxy health)
            throws MCg3ApiOperationIncompleteException {
        super(StatusEvaluationWorkflowRoutine.class, api, health, Version.of(1, 0, 0), Version.of(1, 0, 0),
                "Evaluates numeric metrics against warning and alarm conditions.", "Tessera", "Apache License 2.0", 3L);
        health.setHealthFlagState(HealthFlagState.RUNNING);
        try {
            runRoutine = TesseraRoutineApplicationFactory.create(api, new StatusEvaluationFunctionHandler());
        } catch (Exception e) {
            health.setHealthFlagState(HealthFlagState.FATAL);
            throw new MCg3ApiOperationIncompleteException(e);
        }
    }

    @Override
    public void run() {
        try (AutoCloseable ignored = getWorkflowRoutineApi().getExecutionContext().getMdcContext().use()) {
            healthFlagProxy.setHealthFlagState(HealthFlagState.RUNNING);
            runRoutine.run();
            healthFlagProxy.setHealthFlagState(HealthFlagState.COMPLETE);
        } catch (Throwable t) {
            logger.error("Status evaluation routine failed", t);
            healthFlagProxy.setHealthFlagState(HealthFlagState.FATAL);
            throw new RuntimeException(t);
        }
    }

    @Override
    public void terminate() {
        healthFlagProxy.setHealthFlagState(HealthFlagState.COMPLETE);
    }
}
