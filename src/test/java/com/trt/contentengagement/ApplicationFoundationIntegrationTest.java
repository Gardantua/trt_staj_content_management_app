package com.trt.contentengagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.trt.contentengagement.identity.application.CurrentActor;
import com.trt.contentengagement.identity.application.GetCurrentActorUseCase;
import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Testcontainers
@ActiveProfiles("test")
@Import(ApplicationFoundationIntegrationTest.SecurityProbeConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.prometheus.metrics.export.enabled=true",
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus"
        }
)
class ApplicationFoundationIntegrationTest {

    private static final UUID USER_ACTOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EDITOR_ACTOR_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ADMIN_ACTOR_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withDatabaseName("content_engagement_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ListAppender<ILoggingEvent> temporaryAuthenticationLogAppender;

    @LocalServerPort
    private int serverPort;

    @Autowired
    ApplicationFoundationIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void detachTemporaryAuthenticationLogAppender() {
        if (temporaryAuthenticationLogAppender != null) {
            Logger authenticationFilterLogger = (Logger) LoggerFactory.getLogger(
                    TemporaryHeaderAuthenticationFilter.class
            );
            authenticationFilterLogger.detachAppender(temporaryAuthenticationLogAppender);
            temporaryAuthenticationLogAppender = null;
        }
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
    void prometheusEndpointPublishesHttpLatencyMetrics()
            throws IOException, InterruptedException {
        sendGetRequest("/actuator/health");

        HttpResponse<String> metricsResponse = sendGetRequest("/actuator/prometheus");

        assertThat(metricsResponse.statusCode()).isEqualTo(200);
        assertThat(metricsResponse.body())
                .contains("http_server_requests_seconds_count")
                .contains("application=\"content-engagement-platform\"");
    }

    @Test
    void unknownApiRouteReturnsStableErrorEnvelope() throws IOException, InterruptedException {
        String requestedTraceId = "foundation-test-trace";
        HttpRequest unknownRouteRequest = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/unknown-route"))
                .header("X-Trace-Id", requestedTraceId)
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, USER_ACTOR_ID.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
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

    @Test
    void apiAndSecurityErrorsFollowAcceptLanguageWithoutChangingTheirContracts()
            throws IOException, InterruptedException {
        HttpResponse<String> turkishNotFound = sendLocalizedUnknownRoute("tr", "localized-trace-tr");
        HttpResponse<String> englishNotFound = sendLocalizedUnknownRoute("en-US", "localized-trace-en");
        HttpRequest englishAuthenticationRequest = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/identity/me"))
                .header("Accept-Language", "en")
                .GET()
                .build();
        HttpResponse<String> englishAuthentication = httpClient.send(
                englishAuthenticationRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(turkishNotFound.statusCode()).isEqualTo(404);
        assertThat(turkishNotFound.body())
                .contains("\"code\":\"RESOURCE_NOT_FOUND\"")
                .contains("\"message\":\"İstenen kaynak bulunamadı.\"")
                .contains("\"traceId\":\"localized-trace-tr\"");
        assertThat(englishNotFound.statusCode()).isEqualTo(404);
        assertThat(englishNotFound.body())
                .contains("\"code\":\"RESOURCE_NOT_FOUND\"")
                .contains("\"message\":\"The requested resource was not found.\"")
                .contains("\"traceId\":\"localized-trace-en\"");
        assertThat(englishAuthentication.statusCode()).isEqualTo(401);
        assertThat(englishAuthentication.body())
                .contains("\"code\":\"AUTHENTICATION_REQUIRED\"")
                .contains("\"message\":\"Authentication is required to access this resource.\"")
                .contains("\"traceId\":");
    }

    @Test
    void protectedEndpointRejectsRequestWithoutIdentity()
            throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/api/v1/identity/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"AUTHENTICATION_REQUIRED\"");
        assertThat(response.body()).contains("\"traceId\":");
        assertThat(response.headers().firstValue("X-Trace-Id")).isPresent();
    }

    @Test
    void userEditorAndAdminTestIdentitiesRemainDistinct()
            throws IOException, InterruptedException {
        List<TestIdentity> testIdentities = List.of(
                new TestIdentity(USER_ACTOR_ID, "USER"),
                new TestIdentity(EDITOR_ACTOR_ID, "EDITOR"),
                new TestIdentity(ADMIN_ACTOR_ID, "ADMIN")
        );

        for (TestIdentity testIdentity : testIdentities) {
            HttpResponse<String> response = sendAuthenticatedGetRequest(
                    "/api/v1/identity/me",
                    testIdentity.actorId(),
                    testIdentity.role()
            );

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("\"actorId\":\"" + testIdentity.actorId() + "\"")
                    .contains("\"roles\":[\"" + testIdentity.role() + "\"]");
        }
    }

    @Test
    void malformedTestIdentityIsRejectedWithoutLeakingHeaderValueToLogs()
            throws IOException, InterruptedException {
        String sensitiveInvalidActorId = "secret-token-that-must-not-be-logged";
        temporaryAuthenticationLogAppender = attachTemporaryAuthenticationLogAppender();
        HttpRequest malformedIdentityRequest = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/identity/me"))
                .header(
                        TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER,
                        sensitiveInvalidActorId
                )
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                malformedIdentityRequest,
                HttpResponse.BodyHandlers.ofString()
        );
        String capturedSecurityLogs = temporaryAuthenticationLogAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (allMessages, message) -> allMessages + message);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body())
                .contains("\"code\":\"AUTHENTICATION_INVALID\"")
                .doesNotContain(sensitiveInvalidActorId);
        assertThat(capturedSecurityLogs)
                .contains("Temporary actor id is invalid.")
                .doesNotContain(sensitiveInvalidActorId);
    }

    @Test
    void userRoleCannotInvokeEditorProtectedUseCase()
            throws IOException, InterruptedException {
        HttpResponse<String> response = sendActorSourceProbeRequest(
                USER_ACTOR_ID,
                "USER",
                ADMIN_ACTOR_ID
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"ACCESS_DENIED\"");
    }

    @Test
    void applicationUseCaseUsesVerifiedActorInsteadOfClaimedRequestActor()
            throws IOException, InterruptedException {
        HttpResponse<String> response = sendActorSourceProbeRequest(
                EDITOR_ACTOR_ID,
                "EDITOR",
                ADMIN_ACTOR_ID
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"verifiedActorId\":\"" + EDITOR_ACTOR_ID + "\"")
                .doesNotContain(ADMIN_ACTOR_ID.toString());
    }

    private HttpResponse<String> sendGetRequest(String requestPath)
            throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(createLocalUri(requestPath))
                .GET()
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendLocalizedUnknownRoute(String language, String traceId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/unknown-localized-route"))
                .header("Accept-Language", language)
                .header("X-Trace-Id", traceId)
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, USER_ACTOR_ID.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthenticatedGetRequest(
            String requestPath,
            UUID actorId,
            String role
    ) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(createLocalUri(requestPath))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, actorId.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role)
                .GET()
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendActorSourceProbeRequest(
            UUID authenticatedActorId,
            String role,
            UUID claimedActorId
    ) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(createLocalUri("/api/v1/test/actor-source"))
                .header(
                        TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER,
                        authenticatedActorId.toString()
                )
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"claimedActorId\":\"" + claimedActorId + "\"}"
                ))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private ListAppender<ILoggingEvent> attachTemporaryAuthenticationLogAppender() {
        Logger authenticationFilterLogger = (Logger) LoggerFactory.getLogger(
                TemporaryHeaderAuthenticationFilter.class
        );
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        authenticationFilterLogger.addAppender(logAppender);
        return logAppender;
    }

    private URI createLocalUri(String requestPath) {
        return URI.create("http://localhost:" + serverPort + requestPath);
    }

    private record TestIdentity(UUID actorId, String role) {
    }

    private record ClaimedActorRequest(UUID claimedActorId) {
    }

    private record VerifiedActorResponse(String verifiedActorId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityProbeConfiguration {

        @Bean
        SecurityProbeController securityProbeController(
                GetCurrentActorUseCase getCurrentActorUseCase
        ) {
            return new SecurityProbeController(getCurrentActorUseCase);
        }
    }

    @RestController
    static class SecurityProbeController {

        private final GetCurrentActorUseCase getCurrentActorUseCase;

        SecurityProbeController(GetCurrentActorUseCase getCurrentActorUseCase) {
            this.getCurrentActorUseCase = getCurrentActorUseCase;
        }

        @PostMapping("/api/v1/test/actor-source")
        @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
        VerifiedActorResponse resolveVerifiedActor(
                @RequestBody ClaimedActorRequest claimedActorRequest
        ) {
            CurrentActor currentActor = getCurrentActorUseCase.execute();
            return new VerifiedActorResponse(currentActor.actorId().toString());
        }
    }
}
