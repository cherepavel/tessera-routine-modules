package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.get_data;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.ConfigurationFileScope;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineConfigurationFileDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

@AutoService(RoutineEditorDescriptorProvider.class)
public final class GetDataRoutineEditorDescriptorProvider
        implements RoutineEditorDescriptorProvider {

    @Override
    public RoutineEditorDescriptor getDescriptor() {
        return RoutineEditorDescriptor.newBuilder()
                .routineId("GetDataWorkflowRoutine")
                .displayName("Get Data")
                .description("Generates source metrics and attaches processing reasons.")
                .routineWidgetIds(List.of(BduiWidgetIds.ROUTINE_CONFIGURATION_FILE))
                .configurationFiles(List.of(
                        RoutineConfigurationFileDescriptor.newBuilder()
                                .key("configurationFilePath")
                                .displayName("Generator configuration")
                                .description("JSON configuration used to select and configure the metric generator.")
                                .suggestedFileName("generator_conf.json")
                                .allowedExtensions(List.of(".json"))
                                .allowedScopes(List.of(
                                        ConfigurationFileScope.NODE,
                                        ConfigurationFileScope.PROJECT_GLOBAL
                                ))
                                .defaultScope(ConfigurationFileScope.NODE)
                                .required(true)
                                .build()
                ))
                .functions(List.of(
                        function(
                                "GenerateData",
                                "Generate data",
                                "Generates a metric from the configured data source.",
                                List.of(BduiWidgetIds.FUNC_ENV, BduiWidgetIds.FUNC_OUTPUT_DATA),
                                List.of()
                        ),
                        function(
                                "ProcessReason",
                                "Process reason",
                                "Adds a reason label based on the input status.",
                                List.of(
                                        BduiWidgetIds.FUNC_ENV,
                                        BduiWidgetIds.FUNC_INPUT_DATA,
                                        BduiWidgetIds.FUNC_OUTPUT_DATA
                                ),
                                List.of(
                                        "DataId",
                                        "IgnoreExistsReason",
                                        "PasteReasonWhenOk",
                                        "OkReasonMessage",
                                        "EmptyData",
                                        "GlobalReasonMessage",
                                        "WarningReasonMessage",
                                        "AlarmReasonMessage"
                                )
                        )
                ))
                .build();
    }

    private static RoutineFunctionDescriptor function(
            String id,
            String displayName,
            String description,
            List<String> widgetIds,
            List<String> argumentIds
    ) {
        return RoutineFunctionDescriptor.newBuilder()
                .functionId(id)
                .displayName(displayName)
                .description(description)
                .bduiWidgetIds(widgetIds)
                .argumentIds(argumentIds)
                .build();
    }
}
