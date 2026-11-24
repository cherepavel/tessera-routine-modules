package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEnvironmentDescriptor;

import java.util.List;

/** Describes the generator through the same project IO SPI as the other routines. */
@AutoService(RoutineEditorDescriptorProvider.class)
public final class DataGeneratorRoutineEditorDescriptorProvider implements RoutineEditorDescriptorProvider {
    @Override
    public RoutineEditorDescriptor getDescriptor() {
        return RoutineEditorDescriptor.newBuilder()
                .routineId("DataGeneratorWorkflowRoutine")
                .displayName("Data Generator")
                .description("Generates raw numeric metrics and random statuses for testing pipelines.")
                .routineWidgetIds(List.of(BduiWidgetIds.ROUTINE_ENV))
                .environment(List.of(RoutineEnvironmentDescriptor.newBuilder()
                        .key("Seed")
                        .displayName("Random seed")
                        .description("Optional signed 64-bit seed. Identical scripts and seeds reproduce the sequence; "
                                + "blank uses a random seed.")
                        .required(false)
                        .build()))
                .functions(List.of(
                        RoutineFunctionDescriptor.newBuilder()
                                .functionId("GenerateData")
                                .displayName("Generate data")
                                .description("Generates one random integer status: 0 (OK), 1 (warning), or 2 (alarm).")
                                .bduiWidgetIds(List.of(BduiWidgetIds.FUNC_OUTPUT_DATA))
                                .build(),
                        RoutineFunctionDescriptor.newBuilder()
                                .functionId("GenerateNumber")
                                .displayName("Generate number")
                                .description("Generates a number between Min and Max. Type is INTEGER (inclusive bounds) "
                                        + "or DECIMAL (default, upper-exclusive); equal bounds produce a constant. "
                                        + "MetricName defaults to the lower-case result ID.")
                                .bduiWidgetIds(List.of(BduiWidgetIds.FUNC_ENV, BduiWidgetIds.FUNC_OUTPUT_DATA))
                                .argumentIds(List.of("Min", "Max", "Type", "MetricName", "PromLabel_*"))
                                .build()
                ))
                .build();
    }
}
