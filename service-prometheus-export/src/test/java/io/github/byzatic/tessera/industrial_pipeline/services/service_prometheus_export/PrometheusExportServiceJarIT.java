package io.github.byzatic.tessera.industrial_pipeline.services.service_prometheus_export;

import io.github.byzatic.tessera.lib.configio.infrastructure.factory.ServiceEditorMetadataLoaderFactory;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceStorageRole;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PrometheusExportServiceJarIT {

    @Test
    public void shouldExposeMetadataFromPackagedJar() throws Exception {
        Path artifact = Path.of(System.getProperty("routine.artifact"));
        Path modulesDirectory = Files.createTempDirectory("prometheus-export-service-it-");
        Path isolatedArtifact = Files.copy(artifact, modulesDirectory.resolve(artifact.getFileName()));
        try (var loader = ServiceEditorMetadataLoaderFactory.create(
                modulesDirectory,
                (ClassLoader) null
        )) {
            var metadata = loader.findMetadata("PrometheusExportService").orElseThrow();

            assertEquals("1.0.0", metadata.getVersion());
            assertEquals(isolatedArtifact.getFileName().toString(), metadata.getArtifactFileName());
            assertEquals(
                    List.of("storage", "expiredMinutesAgo", "apiURL", "cronMetricUpdateString"),
                    metadata.getDescriptor().getParameters().stream()
                            .map(parameter -> parameter.getParameterId())
                            .toList()
            );
            assertEquals(
                    ServiceStorageRole.INPUT,
                    metadata.getDescriptor().getParameters().get(0).getStorageRole()
            );
        } finally {
            Files.deleteIfExists(isolatedArtifact);
            Files.deleteIfExists(modulesDirectory);
        }
    }
}
