package io.github.byzatic.tessera.industrial_pipeline.workflowroutine.status_evaluation;

import io.github.byzatic.tessera.lib.configio.infrastructure.factory.RoutineEditorMetadataLoaderFactory;
import io.github.byzatic.tessera.lib.configio.unified.spi.routine.BduiWidgetIds;
import io.github.byzatic.tessera.storageapi.dto.StorageItem;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatusEvaluationRoutineJarIT {
    @Test
    public void exposesMetadataAndFactoryFromPackagedJar() throws Exception {
        Path artifact = Path.of(System.getProperty("routine.artifact"));
        Path directory = Files.createTempDirectory("status-evaluation-jar-it-");
        Path isolated = Files.copy(artifact, directory.resolve(artifact.getFileName()));
        try (var loader = RoutineEditorMetadataLoaderFactory.create(directory, (ClassLoader) null);
             var jar = new JarFile(isolated.toFile())) {
            var metadata = loader.findMetadata("StatusEvaluationWorkflowRoutine").orElseThrow();
            var descriptor = metadata.getDescriptor();
            assertEquals("1.0.0", metadata.getVersion());
            assertEquals(isolated.getFileName().toString(), metadata.getArtifactFileName());
            assertEquals(List.of(), descriptor.getRoutineWidgetIds());
            assertEquals(List.of(), descriptor.getEnvironment());
            assertEquals(List.of(), descriptor.getConfigurationFiles());
            assertEquals(1, descriptor.getFunctions().size());
            var function = descriptor.getFunctions().get(0);
            assertEquals("EvaluateStatus", function.getFunctionId());
            assertEquals(List.of(BduiWidgetIds.FUNC_ENV, BduiWidgetIds.FUNC_INPUT_DATA, BduiWidgetIds.FUNC_OUTPUT_DATA),
                    function.getBduiWidgetIds());
            assertEquals(List.of("DataId", "MetricName", "WarningThreshold", "WarningOperator",
                    "AlarmThreshold", "AlarmOperator", "PromLabel_*"), function.getArgumentIds());
            var entry = jar.getJarEntry("META-INF/services/" + WorkflowRoutineFactoryInterface.class.getName());
            try (var content = jar.getInputStream(entry)) {
                assertEquals(StatusEvaluationWorkflowRoutineFactory.class.getName(),
                        new String(content.readAllBytes(), StandardCharsets.UTF_8).trim());
            }
        } finally {
            Files.deleteIfExists(isolated);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void executesPackagedGenerationEvaluationAndAggregationWithoutOptionalJreModules() throws Exception {
        Path testClasses = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        String classpath = String.join(File.pathSeparator, testClasses.toString(),
                System.getProperty("routine.artifact"), System.getProperty("generator.artifact"),
                System.getProperty("processing.artifact"), System.getProperty("shared.artifact"));
        Path output = Files.createTempFile("status-pipeline-jar-it-", ".log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "--limit-modules", "java.se", "-cp", classpath, PackagedPipelineProbe.class.getName())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue("Packaged pipeline probe timed out", process.waitFor(30, TimeUnit.SECONDS));
            String log = Files.readString(output);
            assertEquals(log, 0, process.exitValue());
            assertTrue(log, log.contains("Packaged numeric status pipeline passed"));
        } finally {
            process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }

    /** Runs with only test helpers and shaded artifacts on the class path, without JUnit. */
    public static final class PackagedPipelineProbe {
        public static void main(String[] args) throws Exception {
            check(ModuleLayer.boot().findModule("jdk.random").isEmpty(), "jdk.random must be absent");
            Map<RoutineTestApi.Key, StorageItem> storage = new HashMap<>();
            var generator = factory("DataGeneratorWorkflowRoutineFactory");
            var evaluator = factory("StatusEvaluationWorkflowRoutineFactory");
            var aggregator = factory("ProcessingStatusWorkflowRoutineFactory");
            List<String> values = List.of("69", "70", "90");
            for (int i = 0; i < values.size(); i++) {
                String generation = """
                        PROCESS FUNCTION GenerateNumber("Min=%s", "Max=%s", "MetricName=temperature") RETURN RAW;
                        PUT DATA RAW TO STORAGE MEASUREMENTS BY DATA ID temperature;
                        """.formatted(values.get(i), values.get(i));
                String node = "Source" + i;
                RoutineTestApi source = new RoutineTestApi(storage, node, generation);
                run(generator, source, "1.1.0");
                check(source.local("MEASUREMENTS", "temperature").getMetricValue().equals(values.get(i)), "Raw value changed");
                String evaluation = """
                        GET FROM SELF STORAGE MEASUREMENTS BY DATA ID temperature AS RAW;
                        PROCESS FUNCTION EvaluateStatus("DataId=RAW", "WarningThreshold=70", "AlarmThreshold=90") RETURN STATUS;
                        PUT DATA STATUS TO STORAGE STATUS BY DATA ID status;
                        """;
                run(evaluator, new RoutineTestApi(storage, node, evaluation), "1.0.0");
                var status = source.local("STATUS", "status");
                check(status.getMetricValue().equals(Integer.toString(i)), "Wrong status at " + values.get(i));
                check(status.getMetricLabels().stream().anyMatch(label -> label.getKey().equals("reason")) == (i > 0),
                        "Wrong reason presence at " + values.get(i));
            }
            String aggregation = """
                    GET FROM CHILD Source0 STORAGE STATUS BY DATA ID status AS A;
                    GET FROM CHILD Source1 STORAGE STATUS BY DATA ID status AS B;
                    GET FROM CHILD Source2 STORAGE STATUS BY DATA ID status AS C;
                    PROCESS FUNCTION ProcessStatus("MetricName=total_status", "DataId=A", "DataId=B", "DataId=C") RETURN TOTAL;
                    PUT DATA TOTAL TO STORAGE TOTAL BY DATA ID total;
                    """;
            RoutineTestApi root = new RoutineTestApi(storage, "dashboard", aggregation);
            run(aggregator, root, null);
            var total = root.local("TOTAL", "total");
            check(total.getMetricValue().equals("2"), "Aggregation must select alarm");
            String reason = total.getMetricLabels().stream().filter(label -> label.getKey().equals("reason"))
                    .findFirst().orElseThrow().getValue();
            check(reason.equals("root.Source1 => temperature [DataId=RAW]=70 matches WARNING condition >= 70"
                    + " *|* root.Source2 => temperature [DataId=RAW]=90 matches ALARM condition >= 90"),
                    "Aggregation lost reasons: " + reason);
            System.out.println("Packaged numeric status pipeline passed");
        }

        private static WorkflowRoutineFactoryInterface factory(String name) {
            return ServiceLoader.load(WorkflowRoutineFactoryInterface.class).stream()
                    .filter(provider -> provider.type().getSimpleName().equals(name))
                    .findFirst().orElseThrow().get();
        }

        private static void run(WorkflowRoutineFactoryInterface factory, RoutineTestApi api, String version) throws Exception {
            HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
            var routine = factory.create(api, health);
            if (version != null) {
                check(routine.getWorkflowRoutineVersion().toString().equals(version), "Wrong runtime version");
            }
            routine.run();
            check(health.getHealthFlagState() == HealthFlagState.COMPLETE, "Routine did not complete");
        }

        private static void check(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
