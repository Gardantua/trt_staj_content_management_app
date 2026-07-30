package com.trt.contentengagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationFoundationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withDatabaseName("content_engagement_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int serverPort;

    @Autowired
    ApplicationFoundationIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void applicationContextStartsAndFlywayAppliesBaselineMigration() {
        Integer successfulBaselineMigrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1' AND success = true
                """,
                Integer.class
        );

        assertThat(successfulBaselineMigrationCount).isEqualTo(1);
    }

    @Test
    void healthEndpointReportsApplicationAsAvailable() throws IOException, InterruptedException {
        HttpResponse<String> healthResponse = sendGetRequest("/actuator/health");

        assertThat(healthResponse.statusCode()).isEqualTo(200);
        assertThat(healthResponse.body()).contains("\"status\":\"UP\"");
        assertThat(healthResponse.headers().firstValue("X-Trace-Id")).isPresent();
    }

    @Test
    void unknownApiRouteReturnsStableErrorEnvelope() throws IOException, InterruptedException {
        String requestedTraceId = "foundation-test-trace";
        HttpRequest unknownRouteRequest = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/unknown-route"))
                .header("X-Trace-Id", requestedTraceId)
                .GET()
                .build();

        HttpResponse<String> errorResponse = httpClient.send(
                unknownRouteRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(errorResponse.statusCode()).isEqualTo(404);
        assertThat(errorResponse.body()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
        assertThat(errorResponse.body()).contains("\"traceId\":\"" + requestedTraceId + "\"");
        assertThat(errorResponse.headers().firstValue("X-Trace-Id"))
                .contains(requestedTraceId);
    }

    private HttpResponse<String> sendGetRequest(String requestPath)
            throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(createLocalUri(requestPath))
                .GET()
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private URI createLocalUri(String requestPath) {
        return URI.create("http://localhost:" + serverPort + requestPath);
    }
}

