package com.trt.contentengagement.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.hikari.connection-timeout=500",
                "spring.datasource.hikari.validation-timeout=500",
                "management.endpoint.health.cache.time-to-live=0"
        }
)
class Stage9OperationalReadinessIntegrationTest {

    private static final String RESTORE_DATABASE = "stage9_restore_probe";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("stage9_operations_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @LocalServerPort
    private int serverPort;

    @Autowired
    Stage9OperationalReadinessIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void ensurePostgresIsAvailable() {
        try {
            POSTGRES.getDockerClient().unpauseContainerCmd(POSTGRES.getContainerId()).exec();
        } catch (RuntimeException ignored) {
            // The container was not paused.
        }
    }

    @Test
    void databaseOutageIsVisibleAndRecoveryPreservesCommittedData() throws Exception {
        UUID marker = UUID.randomUUID();
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS stage9_outage_probe (marker UUID PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO stage9_outage_probe(marker) VALUES (?)", marker);

        POSTGRES.getDockerClient().pauseContainerCmd(POSTGRES.getContainerId()).exec();
        assertThat(healthReportsDatabaseUnavailable()).isTrue();

        POSTGRES.getDockerClient().unpauseContainerCmd(POSTGRES.getContainerId()).exec();
        awaitHealthy();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stage9_outage_probe WHERE marker = ?",
                Integer.class,
                marker
        )).isEqualTo(1);
    }

    @Test
    void postgresqlBackupCanBeRestoredIntoDisposableDatabase() throws Exception {
        UUID marker = UUID.randomUUID();
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS stage9_backup_probe (marker UUID PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO stage9_backup_probe(marker) VALUES (?)", marker);
        int sourceMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );

        assertExecSucceeded(POSTGRES.execInContainer(
                "pg_dump", "-U", POSTGRES.getUsername(), "-Fc",
                "-f", "/tmp/stage9.backup", POSTGRES.getDatabaseName()
        ));
        POSTGRES.execInContainer("dropdb", "-U", POSTGRES.getUsername(), "--if-exists", RESTORE_DATABASE);
        assertExecSucceeded(POSTGRES.execInContainer(
                "createdb", "-U", POSTGRES.getUsername(), RESTORE_DATABASE
        ));
        assertExecSucceeded(POSTGRES.execInContainer(
                "pg_restore", "-U", POSTGRES.getUsername(),
                "-d", RESTORE_DATABASE, "/tmp/stage9.backup"
        ));

        try (Connection restoredConnection = DriverManager.getConnection(
                restoredJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); Statement statement = restoredConnection.createStatement()) {
            try (ResultSet migrationResult = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true"
            )) {
                assertThat(migrationResult.next()).isTrue();
                assertThat(migrationResult.getInt(1)).isEqualTo(sourceMigrationCount);
            }
            try (ResultSet markerResult = statement.executeQuery(
                    "SELECT marker FROM stage9_backup_probe WHERE marker = '" + marker + "'"
            )) {
                assertThat(markerResult.next()).isTrue();
                assertThat(markerResult.getObject(1, UUID.class)).isEqualTo(marker);
            }
        }
    }

    private HttpResponse<String> healthRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/actuator/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean healthReportsDatabaseUnavailable() throws Exception {
        try {
            HttpResponse<String> response = healthRequest();
            return response.statusCode() == 503 && response.body().contains("\"status\":\"DOWN\"");
        } catch (HttpTimeoutException expectedDuringHardOutage) {
            return true;
        }
    }

    private void awaitHealthy() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (healthRequest().statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // Hikari may still be replacing the connection invalidated by the outage.
            }
            Thread.sleep(200);
        }
        throw new AssertionError("PostgreSQL did not recover within 20 seconds");
    }

    private String restoredJdbcUrl() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + RESTORE_DATABASE;
    }

    private void assertExecSucceeded(ExecResult execResult) {
        assertThat(execResult.getExitCode())
                .withFailMessage(execResult.getStderr())
                .isZero();
    }
}
