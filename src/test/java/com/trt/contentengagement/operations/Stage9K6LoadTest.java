package com.trt.contentengagement.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Tag("load")
@org.testcontainers.junit.jupiter.Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage9K6LoadTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("stage9_load_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    @LocalServerPort
    private int serverPort;

    @Test
    void baselineMeetsLatencyErrorAndCapacityGuardrails() throws Exception {
        Testcontainers.exposeHostPorts(serverPort);
        Path script = Path.of("ops", "load", "k6", "stage9-load.js").toAbsolutePath();
        Path result = Path.of("build", "load-results", "baseline-summary.json").toAbsolutePath();
        Files.createDirectories(result.getParent());

        try (GenericContainer<?> k6 = new GenericContainer<>(
                DockerImageName.parse("grafana/k6:2.0.0")
        ).withCopyFileToContainer(MountableFile.forHostPath(script), "/scripts/stage9-load.js")
                .withEnv("BASE_URL", "http://host.testcontainers.internal:" + serverPort)
                .withEnv("LOAD_PROFILE", "baseline")
                .withCommand(
                        "run",
                        "--summary-export=/tmp/baseline-summary.json",
                        "/scripts/stage9-load.js"
                )
                .withStartupCheckStrategy(
                        new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2))
                )) {
            k6.start();
            Long exitCode = k6.getContainerInfo().getState().getExitCodeLong();
            assertThat(exitCode)
                    .withFailMessage(k6.getLogs())
                    .isZero();
            k6.copyFileFromContainer("/tmp/baseline-summary.json", result.toString());
        }

        assertThat(result).exists();
    }
}
