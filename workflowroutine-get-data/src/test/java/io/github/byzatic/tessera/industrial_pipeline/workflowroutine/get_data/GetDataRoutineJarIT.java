package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.get_data;

import io.github.byzatic.tessera.lib.configio.infrastructure.factory.RoutineEditorMetadataLoaderFactory;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.ConfigurationFileScope;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class GetDataRoutineJarIT {

    @Test
    public void shouldExposeMetadataFromPackagedJar() throws Exception {
        Path artifact = Path.of(System.getProperty("routine.artifact"));
        Path modulesDirectory = Files.createTempDirectory("get-data-routine-it-");
        Path isolatedArtifact = Files.copy(artifact, modulesDirectory.resolve(artifact.getFileName()));
        try (var loader = RoutineEditorMetadataLoaderFactory.create(
                modulesDirectory,
                (ClassLoader) null
        )) {
            var metadata = loader.findMetadata("GetDataWorkflowRoutine").orElseThrow();

            assertEquals("1.0.0", metadata.getVersion());
            assertEquals(isolatedArtifact.getFileName().toString(), metadata.getArtifactFileName());
            assertEquals(2, metadata.getDescriptor().getFunctions().size());
            assertEquals(
                    java.util.List.of(BduiWidgetIds.ROUTINE_CONFIGURATION_FILE),
                    metadata.getDescriptor().getRoutineWidgetIds()
            );
            assertEquals(1, metadata.getDescriptor().getConfigurationFiles().size());
            var configurationFile = metadata.getDescriptor().getConfigurationFiles().get(0);
            assertEquals("configurationFilePath", configurationFile.getKey());
            assertEquals("generator_conf.json", configurationFile.getSuggestedFileName());
            assertEquals(java.util.List.of(".json"), configurationFile.getAllowedExtensions());
            assertEquals(
                    java.util.List.of(ConfigurationFileScope.NODE, ConfigurationFileScope.PROJECT_GLOBAL),
                    configurationFile.getAllowedScopes()
            );
            assertEquals(ConfigurationFileScope.NODE, configurationFile.getDefaultScope());
            org.junit.Assert.assertTrue(configurationFile.isRequired());
            org.junit.Assert.assertTrue(metadata.getDescriptor().getEnvironment().isEmpty());
            org.junit.Assert.assertFalse(metadata.getDescriptor().isAllowCustomEnvironmentKeys());
        } finally {
            Files.deleteIfExists(isolatedArtifact);
            Files.deleteIfExists(modulesDirectory);
        }
    }
}
