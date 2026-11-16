package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.application;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.exception.RoutineExecutionException;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.FunctionArguments;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.MetricFunctionHandler;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.RoutineWorkspace;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricLabel;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricSample;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Generates source statuses for testing aggregation nodes. */
public final class DataGeneratorFunctionHandler implements MetricFunctionHandler {
    private final RandomGenerator random;

    /** Uses the supplied random source to generate statuses in the inclusive range 0..2. */
    public DataGeneratorFunctionHandler(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public MetricSample execute(
            String functionName,
            FunctionArguments arguments,
            String resultId,
            RoutineWorkspace workspace
    ) throws RoutineExecutionException {
        if (!"GenerateData".equals(functionName)) {
            throw new RoutineExecutionException("Unknown data-generator function: " + functionName);
        }

        int status = random.nextInt(3);
        List<MetricLabel> labels = status == 0
                ? List.of()
                : List.of(new MetricLabel(
                        "reason",
                        workspace.graphPath() + " => Generated test status: " + status,
                        "="
                ));
        return new MetricSample(
                resultId.toLowerCase(Locale.ROOT),
                Integer.toString(status),
                Instant.now(),
                labels
        );
    }
}
