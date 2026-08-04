package com.trt.contentengagement.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=true",
                "app.rate-limit.capacity=2",
                "app.rate-limit.refill-tokens=1",
                "app.rate-limit.refill-period=1h",
                "app.rate-limit.max-tracked-clients=100"
        }
)
class RateLimitingIntegrationTest {
    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:17.5-alpine")
            .withDatabaseName("rate_limit_test")
            .withUsername("content_engagement")
            .withPassword("test_password");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final MeterRegistry meterRegistry;

    @LocalServerPort
    private int serverPort;

    @Autowired
    RateLimitingIntegrationTest(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Test
    void thirdApiRequestIsRejectedWithStableEnvelopeAndRetryHeaders() throws Exception {
        HttpResponse<String> first = sendIdentityRequest("rate-limit-trace-1");
        HttpResponse<String> second = sendIdentityRequest("rate-limit-trace-2");
        HttpResponse<String> rejected = sendIdentityRequest("rate-limit-trace-3");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(rejected.body())
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"traceId\":\"rate-limit-trace-3\"");
        assertThat(rejected.headers().firstValue("RateLimit-Limit")).contains("2");
        assertThat(rejected.headers().firstValue("RateLimit-Remaining")).contains("0");
        assertThat(rejected.headers().firstValue("Retry-After")).contains("3600");
        assertThat(meterRegistry.get("http.rate.limit.rejected").counter().count())
                .isEqualTo(1);
    }

    private HttpResponse<String> sendIdentityRequest(String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/api/v1/identity/me"))
                .header(TraceHeaderNames.REQUEST_TRACE_ID, traceId)
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, USER_ID.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static final class TraceHeaderNames {
        private static final String REQUEST_TRACE_ID = "X-Trace-Id";
    }
}
