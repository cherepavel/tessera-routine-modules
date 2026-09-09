package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.graph_lifting_data;

import io.github.byzatic.tessera.lib.configio.infrastructure.factory.RoutineEditorMetadataLoaderFactory;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class GraphLiftingDataRoutineJarIT {

    @Test
    public void shouldExposeMetadataFromPackagedJar() throws Exception {
        Path artifact = Path.of(System.getProperty("routine.artifact"));
        Path modulesDirectory = Files.createTempDirectory("graph-lifting-routine-it-");
        Path isolatedArtifact = Files.copy(artifact, modulesDirectory.resolve(artifact.getFileName()));
        try (var loader = RoutineEditorMetadataLoaderFactory.create(
                modulesDirectory,
                (ClassLoader) null
        )) {
            var metadata = loader.findMetadata("GraphLiftingDataWorkflowRoutine").orElseThrow();

            assertEquals("1.0.0", metadata.getVersion());
            assertEquals(isolatedArtifact.getFileName().toString(), metadata.getArtifactFileName());
            assertEquals("LiftData", metadata.getDescriptor().getFunctions().get(0).getFunctionId());
        } finally {
            Files.deleteIfExists(isolatedArtifact);
            Files.deleteIfExists(modulesDirectory);
        }
    }
}
