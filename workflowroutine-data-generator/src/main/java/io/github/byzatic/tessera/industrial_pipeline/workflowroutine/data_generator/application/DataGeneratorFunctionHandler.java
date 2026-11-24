package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.application;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.exception.RoutineExecutionException;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.FunctionArguments;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.MetricFunctionHandler;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.RoutineWorkspace;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricLabel;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricSample;
import io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator.domain.NumericRange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Generates raw numeric metrics and backward-compatible source statuses. */
public final class DataGeneratorFunctionHandler implements MetricFunctionHandler {
    private final RandomGenerator random;

    /** Uses the supplied random source for both raw numeric values and legacy 0..2 statuses. */
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
        return switch (functionName) {
            case "GenerateData" -> generateStatus(resultId, workspace);
            case "GenerateNumber" -> generateNumber(arguments, resultId);
            default -> throw new RoutineExecutionException("Unknown data-generator function: " + functionName);
        };
    }

    private MetricSample generateStatus(String resultId, RoutineWorkspace workspace) throws RoutineExecutionException {
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

    private MetricSample generateNumber(FunctionArguments arguments, String resultId) {
        for (String key : List.of("Min", "Max", "Type", "MetricName")) {
            if (arguments.all(key).size() > 1) {
                throw new IllegalArgumentException("Argument must be specified once: " + key);
            }
        }
        NumericRange.ValueType type;
        try {
            type = NumericRange.ValueType.valueOf(arguments.optional("Type").orElse("DECIMAL").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type must be INTEGER or DECIMAL", e);
        }
        NumericRange range = new NumericRange(decimal(arguments, "Min"), decimal(arguments, "Max"), type);
        String metricName = arguments.optional("MetricName").orElse(resultId.toLowerCase(Locale.ROOT));
        if (metricName.isBlank()) {
            throw new IllegalArgumentException("MetricName must not be blank");
        }
        return new MetricSample(
                metricName,
                range.generate(random).stripTrailingZeros().toPlainString(),
                Instant.now(),
                arguments.prometheusLabels()
        );
    }

    private static BigDecimal decimal(FunctionArguments arguments, String key) {
        try {
            return new BigDecimal(arguments.required(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a finite decimal number", e);
        }
    }
}
