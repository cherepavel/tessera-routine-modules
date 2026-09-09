package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.graph_lifting_data;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.RoutineFunctionDescriptor;

import java.util.List;

@AutoService(RoutineEditorDescriptorProvider.class)
public final class GraphLiftingDataRoutineEditorDescriptorProvider
        implements RoutineEditorDescriptorProvider {

    @Override
    public RoutineEditorDescriptor getDescriptor() {
        RoutineFunctionDescriptor liftData = RoutineFunctionDescriptor.newBuilder()
                .functionId("LiftData")
                .displayName("Lift data")
                .description("Loads metric data from a downstream node.")
                .bduiWidgetIds(List.of(
                        BduiWidgetIds.INPUT_FROM_DOWNSTREAM,
                        BduiWidgetIds.FUNC_OUTPUT_DATA
                ))
                .argumentIds(List.of("NodeId", "NodeStorage", "DataId"))
                .build();
        return RoutineEditorDescriptor.newBuilder()
                .routineId("GraphLiftingDataWorkflowRoutine")
                .displayName("Graph Lifting Data")
                .description("Lifts metric data from downstream nodes into the current pipeline.")
                .functions(List.of(liftData))
                .build();
    }
}
