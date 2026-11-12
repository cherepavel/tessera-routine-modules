package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import com.github.zafarkhaja.semver.Version;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.usecase.RunRoutineUseCase;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.composition.TesseraRoutineApplicationFactory;
import io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.application.DataGeneratorFunctionHandler;
import io.github.byzatic.tessera.storageapi.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.AbstractWorkflowRoutine;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.random.RandomGenerator;

/** Composition root for the test data generator routine. */
public final class DataGeneratorWorkflowRoutine extends AbstractWorkflowRoutine {
    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorWorkflowRoutine.class);

    private final RunRoutineUseCase runRoutine;

    public DataGeneratorWorkflowRoutine(
            MCg3WorkflowRoutineApiInterface workflowRoutineApi,
            HealthFlagProxy healthFlagProxy
    ) throws MCg3ApiOperationIncompleteException {
        super(
                DataGeneratorWorkflowRoutine.class,
                workflowRoutineApi,
                healthFlagProxy,
                Version.of(1, 0, 0),
                Version.of(1, 0, 0),
                "Generates random statuses from 0 to 2 for testing aggregation nodes.",
                "Tessera",
                "Apache License 2.0",
                3L
        );
        healthFlagProxy.setHealthFlagState(HealthFlagState.RUNNING);
        try {
            runRoutine = TesseraRoutineApplicationFactory.create(
                    workflowRoutineApi,
                    new DataGeneratorFunctionHandler(RandomGenerator.getDefault())
            );
        } catch (Exception e) {
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
            logger.error("Data generator routine failed", t);
            healthFlagProxy.setHealthFlagState(HealthFlagState.FATAL);
            throw new RuntimeException(t);
        }
    }

    @Override
    public void terminate() {
        healthFlagProxy.setHealthFlagState(HealthFlagState.COMPLETE);
    }
}
