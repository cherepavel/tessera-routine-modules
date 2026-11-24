package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.application;

import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.exception.RoutineExecutionException;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.model.FunctionArguments;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.MetricFunctionHandler;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.application.service.RoutineWorkspace;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricLabel;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.model.MetricSample;
import io.github.byzatic.tessera.industrial_pipeline.sharedresources.project_common.domain.service.MetricLabels;
import io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.domain.NumericCondition;
import io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation.domain.StatusRules;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Maps DSL arguments and source metrics to exact domain comparisons. */
public final class StatusEvaluationFunctionHandler implements MetricFunctionHandler {
    @Override
    public MetricSample execute(String functionName, FunctionArguments arguments, String resultId,
                                RoutineWorkspace workspace) throws RoutineExecutionException {
        if (!"EvaluateStatus".equals(functionName)) {
            throw new RoutineExecutionException("Unknown status-evaluation function: " + functionName);
        }
        for (String key : List.of("DataId", "MetricName", "WarningThreshold", "WarningOperator",
                "AlarmThreshold", "AlarmOperator")) {
            if (arguments.all(key).size() > 1) {
                throw new IllegalArgumentException("Argument must be specified once: " + key);
            }
        }
        StatusRules rules = new StatusRules(condition(arguments, "Warning"), condition(arguments, "Alarm"));
        String dataId = arguments.required("DataId");
        MetricSample source = workspace.metric(dataId);
        BigDecimal value = decimal(source.value().trim(), "Metric " + dataId);
        StatusRules.Evaluation evaluation = rules.evaluate(value);
        String metricName = arguments.optional("MetricName").orElse(resultId.toLowerCase(Locale.ROOT));
        if (metricName.isBlank()) {
            throw new IllegalArgumentException("MetricName must not be blank");
        }
        List<MetricLabel> labels = MetricLabels.remove(
                MetricLabels.upsert(source.labels(), arguments.prometheusLabels()), Set.of("reason"));
        if (evaluation.condition().isPresent()) {
            String reason = workspace.graphPath() + " => " + source.name() + " [DataId=" + dataId + "]="
                    + source.value() + " matches " + evaluation.severity().name() + " condition "
                    + evaluation.condition().orElseThrow().describe();
            labels = MetricLabels.upsert(labels, List.of(new MetricLabel("reason", reason, "=")));
        }
        return new MetricSample(metricName, evaluation.severity().value(), Instant.now(), labels);
    }

    private static Optional<NumericCondition> condition(FunctionArguments arguments, String prefix) {
        Optional<String> threshold = arguments.optional(prefix + "Threshold");
        Optional<String> operator = arguments.optional(prefix + "Operator");
        if (threshold.isEmpty()) {
            if (operator.isPresent()) {
                throw new IllegalArgumentException(prefix + "Operator requires " + prefix + "Threshold");
            }
            return Optional.empty();
        }
        return Optional.of(new NumericCondition(
                NumericCondition.Operator.fromSymbol(operator.orElse(">=")),
                decimal(threshold.get(), prefix + "Threshold")
        ));
    }

    private static BigDecimal decimal(String value, String description) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(description + " must be a finite decimal number: " + value, e);
        }
    }
}
