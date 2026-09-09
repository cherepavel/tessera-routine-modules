package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_enrichment;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

@AutoService(RoutineEditorDescriptorProvider.class)
public final class DataEnrichmentRoutineEditorDescriptorProvider
        implements RoutineEditorDescriptorProvider {
    private static final List<String> STANDARD_WIDGETS = List.of(
            BduiWidgetIds.FUNC_ENV,
            BduiWidgetIds.FUNC_INPUT_DATA,
            BduiWidgetIds.FUNC_OUTPUT_DATA
    );

    @Override
    public RoutineEditorDescriptor getDescriptor() {
        return RoutineEditorDescriptor.newBuilder()
                .routineId("DataEnrichmentWorkflowRoutine")
                .displayName("Data Enrichment")
                .description("Enriches and transforms metric data.")
                .functions(List.of(
                        function(
                                "AddGraphPath",
                                "Add graph path",
                                "Adds the current graph path as a metric label.",
                                List.of("DataId")
                        ),
                        function(
                                "AddLabel",
                                "Add label",
                                "Adds or replaces Prometheus labels on a metric.",
                                List.of("DataId", "PromLabel_*")
                        ),
                        function(
                                "ModifyMetric",
                                "Modify metric",
                                "Changes a metric name, value, timestamp, and labels.",
                                List.of(
                                        "DataId",
                                        "RemoveAllLabels",
                                        "RemoveLabelByName",
                                        "NewMetricName",
                                        "NewMetricValue",
                                        "SetCreationTimeNow",
                                        "PromLabel_*"
                                )
                        )
                ))
                .build();
    }

    private static RoutineFunctionDescriptor function(
            String id,
            String displayName,
            String description,
            List<String> argumentIds
    ) {
        return RoutineFunctionDescriptor.newBuilder()
                .functionId(id)
                .displayName(displayName)
                .description(description)
                .bduiWidgetIds(STANDARD_WIDGETS)
                .argumentIds(argumentIds)
                .build();
    }
}
