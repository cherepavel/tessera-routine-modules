package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

/** Declares status evaluation using existing input, parameter and output widgets. */
@AutoService(RoutineEditorDescriptorProvider.class)
public final class StatusEvaluationRoutineEditorDescriptorProvider implements RoutineEditorDescriptorProvider {
    @Override
    public RoutineEditorDescriptor getDescriptor() {
        return RoutineEditorDescriptor.newBuilder()
                .routineId("StatusEvaluationWorkflowRoutine")
                .displayName("Status Evaluation")
                .description("Computes OK, warning or alarm from raw numeric metrics.")
                .functions(List.of(RoutineFunctionDescriptor.newBuilder()
                        .functionId("EvaluateStatus")
                        .displayName("Evaluate status")
                        .description("Evaluates one DataId against WarningThreshold/AlarmThreshold. "
                                + "Operators default to >= and support >, >=, <, <=, ==, !=. "
                                + "At least one threshold is required; alarm wins. Invalid or missing data fails execution. "
                                + "MetricName defaults to the lower-case result ID.")
                        .bduiWidgetIds(List.of(BduiWidgetIds.FUNC_ENV, BduiWidgetIds.FUNC_INPUT_DATA,
                                BduiWidgetIds.FUNC_OUTPUT_DATA))
                        .argumentIds(List.of("DataId", "MetricName", "WarningThreshold", "WarningOperator",
                                "AlarmThreshold", "AlarmOperator", "PromLabel_*"))
                        .build()))
                .build();
    }
}
