package com.trt.contentengagement.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import com.trt.contentengagement.identity.application.PasswordResetNotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AccountAuthenticationIntegrationTest.PasswordResetTestConfiguration.class)
class AccountAuthenticationIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withDatabaseName("content_engagement_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final RecordingPasswordResetNotification passwordResetNotification;

    @LocalServerPort
    private int serverPort;

    @Autowired
    AccountAuthenticationIntegrationTest(
            JdbcTemplate jdbcTemplate,
            RecordingPasswordResetNotification passwordResetNotification
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordResetNotification = passwordResetNotification;
    }

    @Test
    void registrationCreatesUserSessionAndStoresOnlyPasswordHash() throws Exception {
        HttpClient sessionClient = sessionClient();
        HttpResponse<String> registration = postJson(
                sessionClient,
                "/api/v1/auth/register",
                """
                {"email":"Yunus@example.com","displayName":"Yunus","password":"secret123"}
                """
        );

        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(registration.body())
                .contains("\"email\":\"Yunus@example.com\"")
                .contains("\"displayName\":\"Yunus\"")
                .contains("\"roles\":[\"USER\"]")
                .doesNotContain("secret123");

        String actorId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM identity_user_accounts WHERE normalized_email = 'yunus@example.com'",
                String.class
        );
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM identity_user_accounts WHERE normalized_email = 'yunus@example.com'",
                String.class
        );
        assertThat(passwordHash).startsWith("{bcrypt}").doesNotContain("secret123");

        HttpResponse<String> currentAccount = get(sessionClient, "/api/v1/auth/me");
        HttpResponse<String> currentActor = get(sessionClient, "/api/v1/identity/me");
        assertThat(currentAccount.statusCode()).isEqualTo(200);
        assertThat(currentAccount.body()).contains("\"actorId\":\"" + actorId + "\"");
        assertThat(currentActor.body()).contains("\"actorId\":\"" + actorId + "\"");
    }

    @Test
    void loginUsesGenericFailureAndLogoutInvalidatesSession() throws Exception {
        HttpClient registrationClient = sessionClient();
        postJson(
                registrationClient,
                "/api/v1/auth/register",
                """
                {"email":"login@example.com","displayName":"Login User","password":"secret123"}
                """
        );

        HttpClient loginClient = sessionClient();
        HttpResponse<String> invalidLogin = postJson(
                loginClient,
                "/api/v1/auth/login",
                """
                {"email":"login@example.com","password":"wrong-password"}
                """
        );
        assertThat(invalidLogin.statusCode()).isEqualTo(401);
        assertThat(invalidLogin.body())
                .contains("\"code\":\"AUTHENTICATION_INVALID\"")
                .doesNotContain("login@example.com");

        HttpResponse<String> validLogin = postJson(
                loginClient,
                "/api/v1/auth/login",
                """
                {"email":"LOGIN@example.com","password":"secret123"}
                """
        );
        assertThat(validLogin.statusCode()).isEqualTo(200);
        assertThat(get(loginClient, "/api/v1/auth/me").statusCode()).isEqualTo(200);

        HttpResponse<String> logout = postJson(loginClient, "/api/v1/auth/logout", "");
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(get(loginClient, "/api/v1/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void duplicateEmailAndInvalidRegistrationAreRejected() throws Exception {
        HttpClient client = sessionClient();
        String accountBody = """
                {"email":"same@example.com","displayName":"Same User","password":"secret123"}
                """;
        assertThat(postJson(client, "/api/v1/auth/register", accountBody).statusCode()).isEqualTo(201);

        HttpResponse<String> duplicate = postJson(client, "/api/v1/auth/register", accountBody);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"code\":\"ACCOUNT_EMAIL_ALREADY_USED\"");

        HttpResponse<String> weakPassword = postJson(
                sessionClient(),
                "/api/v1/auth/register",
                """
                {"email":"weak@example.com","displayName":"Weak User","password":"short"}
                """
        );
        assertThat(weakPassword.statusCode()).isEqualTo(400);
        assertThat(weakPassword.body()).contains("\"code\":\"VALIDATION_FAILED\"");
    }

    @Test
    void passwordResetIsGenericSingleUseAndStoresOnlyTokenHash() throws Exception {
        postJson(
                sessionClient(),
                "/api/v1/auth/register",
                """
                {"email":"reset@example.com","displayName":"Reset User","password":"secret123"}
                """
        );
        passwordResetNotification.clear();

        HttpResponse<String> unknownAccountRequest = postJson(
                sessionClient(),
                "/api/v1/auth/password-reset/request",
                "{\"email\":\"unknown@example.com\"}"
        );
        HttpResponse<String> resetRequest = postJson(
                sessionClient(),
                "/api/v1/auth/password-reset/request",
                "{\"email\":\"reset@example.com\"}"
        );

        assertThat(unknownAccountRequest.statusCode()).isEqualTo(202);
        assertThat(resetRequest.statusCode()).isEqualTo(202);
        assertThat(resetRequest.body()).isEmpty();
        String rawToken = passwordResetNotification.rawToken();
        assertThat(rawToken).isNotBlank();
        String storedTokenHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM identity_password_reset_tokens",
                String.class
        );
        assertThat(storedTokenHash).hasSize(64).isNotEqualTo(rawToken);

        HttpResponse<String> reset = postJson(
                sessionClient(),
                "/api/v1/auth/password-reset/reset",
                "{\"token\":\"%s\",\"newPassword\":\"new-secret-123\"}".formatted(rawToken)
        );
        assertThat(reset.statusCode()).isEqualTo(204);

        HttpResponse<String> oldPasswordLogin = postJson(
                sessionClient(),
                "/api/v1/auth/login",
                "{\"email\":\"reset@example.com\",\"password\":\"secret123\"}"
        );
        HttpResponse<String> newPasswordLogin = postJson(
                sessionClient(),
                "/api/v1/auth/login",
                "{\"email\":\"reset@example.com\",\"password\":\"new-secret-123\"}"
        );
        assertThat(oldPasswordLogin.statusCode()).isEqualTo(401);
        assertThat(newPasswordLogin.statusCode()).isEqualTo(200);

        HttpResponse<String> reusedToken = postJson(
                sessionClient(),
                "/api/v1/auth/password-reset/reset",
                "{\"token\":\"%s\",\"newPassword\":\"another-secret\"}".formatted(rawToken)
        );
        assertThat(reusedToken.statusCode()).isEqualTo(400);
        assertThat(reusedToken.body()).contains("\"code\":\"PASSWORD_RESET_TOKEN_INVALID\"");
    }

    private HttpClient sessionClient() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(localUri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(HttpClient client, String path, String body) throws Exception {
        HttpRequest.BodyPublisher bodyPublisher = body.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder(localUri(path))
                .header("Content-Type", "application/json")
                .POST(bodyPublisher)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI localUri(String path) {
        return URI.create("http://localhost:" + serverPort + path);
    }

    @TestConfiguration
    static class PasswordResetTestConfiguration {
        @Bean
        @Primary
        RecordingPasswordResetNotification passwordResetNotification() {
            return new RecordingPasswordResetNotification();
        }
    }

    static class RecordingPasswordResetNotification implements PasswordResetNotificationPort {
        private final AtomicReference<String> rawToken = new AtomicReference<>();

        @Override
        public void sendPasswordResetLink(String recipientEmail, String token) {
            rawToken.set(token);
        }

        String rawToken() {
            return rawToken.get();
        }

        void clear() {
            rawToken.set(null);
        }
    }
}
