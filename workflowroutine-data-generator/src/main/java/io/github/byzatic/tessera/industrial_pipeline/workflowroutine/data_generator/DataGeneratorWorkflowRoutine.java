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

import java.util.Random;

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
                Version.of(1, 1, 0),
                Version.of(1, 0, 0),
                "Generates raw numeric metrics and random statuses for testing pipelines.",
                "Tessera",
                "Apache License 2.0",
                3L
        );
        healthFlagProxy.setHealthFlagState(HealthFlagState.RUNNING);
        try {
            runRoutine = TesseraRoutineApplicationFactory.create(
                    workflowRoutineApi,
                    new DataGeneratorFunctionHandler(configuredRandom(workflowRoutineApi))
            );
        } catch (Exception e) {
            healthFlagProxy.setHealthFlagState(HealthFlagState.FATAL);
            throw new MCg3ApiOperationIncompleteException(e);
        }
    }

    private static Random configuredRandom(MCg3WorkflowRoutineApiInterface api) {
        var seeds = api.getConfigurationParameters().stream()
                .filter(parameter -> "Seed".equals(parameter.getParameterKey())).toList();
        if (seeds.size() > 1) {
            throw new IllegalArgumentException("Seed must be specified once");
        }
        if (seeds.isEmpty() || seeds.get(0).getParameterValue().isBlank()) {
            return new Random();
        }
        try {
            return new Random(Long.parseLong(seeds.get(0).getParameterValue().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Seed must be a signed 64-bit integer", e);
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
