package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

/** Describes the generator through the same project IO SPI as the other routines. */
@AutoService(RoutineEditorDescriptorProvider.class)
public final class DataGeneratorRoutineEditorDescriptorProvider implements RoutineEditorDescriptorProvider {
    @Override
    public RoutineEditorDescriptor getDescriptor() {
        return RoutineEditorDescriptor.newBuilder()
                .routineId("DataGeneratorWorkflowRoutine")
                .displayName("Data Generator")
                .description("Generates random statuses from 0 to 2 for testing aggregation nodes.")
                .functions(List.of(
                        RoutineFunctionDescriptor.newBuilder()
                                .functionId("GenerateData")
                                .displayName("Generate data")
                                .description("Generates one random integer status: 0 (OK), 1 (warning), or 2 (alarm).")
                                .bduiWidgetIds(List.of(BduiWidgetIds.FUNC_OUTPUT_DATA))
                                .build()
                ))
                .build();
    }
}
