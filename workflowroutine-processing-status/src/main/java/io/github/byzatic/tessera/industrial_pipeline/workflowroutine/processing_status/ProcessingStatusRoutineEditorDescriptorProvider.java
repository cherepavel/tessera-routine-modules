package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.processing_status;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

@AutoService(RoutineEditorDescriptorProvider.class)
public final class ProcessingStatusRoutineEditorDescriptorProvider
        implements RoutineEditorDescriptorProvider {

    @Override
    public RoutineEditorDescriptor getDescriptor() {
        RoutineFunctionDescriptor processStatus = RoutineFunctionDescriptor.newBuilder()
                .functionId("ProcessStatus")
                .displayName("Process status")
                .description("Aggregates input statuses into a single metric.")
                .bduiWidgetIds(List.of(
                        BduiWidgetIds.FUNC_ENV,
                        BduiWidgetIds.FUNC_INPUT_DATA,
                        BduiWidgetIds.FUNC_OUTPUT_DATA
                ))
                .argumentIds(List.of("MetricName", "DataId", "PromLabel_*"))
                .build();
        return RoutineEditorDescriptor.newBuilder()
                .routineId("ProcessingStatusWorkflowRoutine")
                .displayName("Processing Status")
                .description("Aggregates and publishes processing status metrics.")
                .functions(List.of(processStatus))
                .build();
    }
}
