package io.github.byzatic.tessera.industrial_pipeline.services.service_prometheus_export;

import com.google.auto.service.AutoService;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceEditorDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceEditorDescriptorProvider;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceParameterDescriptor;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceParameterType;
import io.github.byzatic.tessera.lib.configio.unified.spi.service.ServiceStorageRole;

import java.util.List;

@AutoService(ServiceEditorDescriptorProvider.class)
public final class PrometheusExportServiceEditorDescriptorProvider
        implements ServiceEditorDescriptorProvider {

    @Override
    public ServiceEditorDescriptor getDescriptor() {
        return ServiceEditorDescriptor.newBuilder()
                .serviceId("PrometheusExportService")
                .displayName("Prometheus Export")
                .description("Publishes metrics from Tessera storage through a Prometheus HTTP endpoint.")
                .parameters(List.of(
                        parameter(
                                "storage",
                                "Input storage",
                                "Storage containing metrics to export.",
                                ServiceParameterType.STRING,
                                "",
                                ServiceStorageRole.INPUT
                        ),
                        parameter(
                                "expiredMinutesAgo",
                                "Retention, minutes",
                                "Maximum metric age in minutes.",
                                ServiceParameterType.LONG,
                                "5",
                                ServiceStorageRole.NONE
                        ),
                        parameter(
                                "apiURL",
                                "Metrics URL",
                                "HTTP endpoint URL including the listening port.",
                                ServiceParameterType.STRING,
                                "http://localhost:8080/metrics",
                                ServiceStorageRole.NONE
                        ),
                        parameter(
                                "cronMetricUpdateString",
                                "Update schedule",
                                "Cron expression controlling metric synchronization.",
                                ServiceParameterType.STRING,
                                "0 */1 * * * *",
                                ServiceStorageRole.NONE
                        )
                ))
                .build();
    }

    private static ServiceParameterDescriptor parameter(
            String id,
            String displayName,
            String description,
            ServiceParameterType type,
            String defaultValue,
            ServiceStorageRole storageRole
    ) {
        return ServiceParameterDescriptor.newBuilder()
                .parameterId(id)
                .displayName(displayName)
                .description(description)
                .type(type)
                .defaultValue(defaultValue)
                .storageRole(storageRole)
                .build();
    }
}
