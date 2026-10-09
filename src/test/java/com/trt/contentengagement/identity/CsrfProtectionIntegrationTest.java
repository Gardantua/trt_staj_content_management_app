package com.trt.contentengagement.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.csrf-enabled=true"
)
class CsrfProtectionIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withDatabaseName("content_engagement_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    @LocalServerPort
    int serverPort;

    @Test
    void rejectsUnsafeRequestWithoutTokenAndAcceptsTheIssuedToken() throws Exception {
        HttpClient unprotectedClient = HttpClient.newHttpClient();
        HttpResponse<String> rejected = unprotectedClient.send(
                registrationRequest(null, null, "tr"), HttpResponse.BodyHandlers.ofString()
        );
        assertThat(rejected.statusCode()).isEqualTo(403);
        assertThat(rejected.body())
                .contains("\"code\":\"ACCESS_DENIED\"")
                .contains("\"message\":\"Bu işlemi yapmaya yetkiniz bulunmuyor.\"")
                .contains("\"traceId\":");

        CookieManager cookieManager = new CookieManager();
        HttpClient protectedClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
        HttpResponse<String> csrfResponse = protectedClient.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        JsonNode csrfToken = new ObjectMapper().readTree(csrfResponse.body());
        String cookieToken = cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .findFirst()
                .orElseThrow()
                .getValue();
        HttpResponse<String> accepted = protectedClient.send(
                registrationRequest(
                        csrfToken.get("headerName").asText(),
                        cookieToken,
                        "en"
                ),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(csrfResponse.statusCode()).isEqualTo(200);
        assertThat(accepted.statusCode()).isEqualTo(201);
        assertThat(accepted.body()).contains("\"roles\":[\"USER\"]");
    }

    private HttpRequest registrationRequest(
            String csrfHeaderName,
            String csrfToken,
            String language
    ) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .header("Accept-Language", language)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"email":"csrf@example.com","displayName":"CSRF User","password":"secret123"}
                        """));
        if (csrfToken != null) {
            request.header(csrfHeaderName, csrfToken);
        }
        return request.build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + serverPort + path);
    }
}
