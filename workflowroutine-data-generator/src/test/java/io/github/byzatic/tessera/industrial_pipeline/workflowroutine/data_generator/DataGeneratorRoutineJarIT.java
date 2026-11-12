package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.data_generator;

import io.github.byzatic.tessera.lib.configio.infrastructure.factory.RoutineEditorMetadataLoaderFactory;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;

public class DataGeneratorRoutineJarIT {
    @Test
    public void exposesEditorMetadataAndRuntimeFactoryFromPackagedJar() throws Exception {
        Path artifact = Path.of(System.getProperty("routine.artifact"));
        Path modulesDirectory = Files.createTempDirectory("data-generator-routine-it-");
        Path isolatedArtifact = Files.copy(artifact, modulesDirectory.resolve(artifact.getFileName()));
        try (var loader = RoutineEditorMetadataLoaderFactory.create(modulesDirectory, (ClassLoader) null);
             var jar = new JarFile(isolatedArtifact.toFile())) {
            var metadata = loader.findMetadata("DataGeneratorWorkflowRoutine").orElseThrow();
            var descriptor = metadata.getDescriptor();

            assertEquals("1.0.0", metadata.getVersion());
            assertEquals(isolatedArtifact.getFileName().toString(), metadata.getArtifactFileName());
            assertEquals(List.of(), descriptor.getConfigurationFiles());
            assertEquals(List.of(), descriptor.getEnvironment());
            assertEquals(1, descriptor.getFunctions().size());
            var function = descriptor.getFunctions().get(0);
            assertEquals("GenerateData", function.getFunctionId());
            assertEquals(List.of(), function.getArgumentIds());
            assertEquals(List.of(BduiWidgetIds.FUNC_OUTPUT_DATA), function.getBduiWidgetIds());

            var factoryRegistration = jar.getJarEntry("META-INF/services/io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface");
            try (var content = jar.getInputStream(factoryRegistration)) {
                assertEquals(DataGeneratorWorkflowRoutineFactory.class.getName(), new String(
                        content.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8
                ).trim());
            }
        } finally {
            Files.deleteIfExists(isolatedArtifact);
            Files.deleteIfExists(modulesDirectory);
        }
    }
}
