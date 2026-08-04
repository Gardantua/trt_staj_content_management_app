package com.trt.contentengagement.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaIntegrationTest {
    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EDITOR_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("media_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int serverPort;

    @Autowired
    MediaIntegrationTest(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM admin_audit_entries");
        jdbcTemplate.update("DELETE FROM media_assets");
    }

    @Test
    void editorUploadsValidatedImageAndAuthenticatedUserReadsSameBytes() throws Exception {
        HttpResponse<byte[]> uploadResponse = upload(
                ONE_PIXEL_PNG, "image/png", EDITOR_ID, "EDITOR"
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(201);
        JsonNode responseJson = objectMapper.readTree(uploadResponse.body());
        String contentUrl = responseJson.get("contentUrl").stringValue();
        assertThat(responseJson.get("width").intValue()).isEqualTo(1);
        assertThat(responseJson.get("height").intValue()).isEqualTo(1);

        HttpRequest readRequest = authenticatedRequest(contentUrl, USER_ID, "USER").GET().build();
        HttpResponse<byte[]> readResponse = httpClient.send(
                readRequest, HttpResponse.BodyHandlers.ofByteArray()
        );
        assertThat(readResponse.statusCode()).isEqualTo(200);
        assertThat(readResponse.headers().firstValue("Content-Type")).contains("image/png");
        assertThat(readResponse.body()).isEqualTo(ONE_PIXEL_PNG);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_assets", Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE action = 'MEDIA_IMAGE_UPLOADED'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void fileSignatureMustMatchDeclaredMimeType() throws Exception {
        HttpResponse<byte[]> response = upload(
                ONE_PIXEL_PNG, "image/jpeg", EDITOR_ID, "EDITOR"
        );

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(new String(response.body(), StandardCharsets.UTF_8))
                .contains("MEDIA_MIME_MISMATCH");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_assets", Integer.class
        )).isZero();
    }

    @Test
    void normalUserCannotUploadMedia() throws Exception {
        HttpResponse<byte[]> response = upload(ONE_PIXEL_PNG, "image/png", USER_ID, "USER");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_assets", Integer.class
        )).isZero();
    }

    @Test
    void oversizedUploadReturnsStablePayloadTooLargeError() throws Exception {
        byte[] oversizedContent = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(ONE_PIXEL_PNG, 0, oversizedContent, 0, ONE_PIXEL_PNG.length);

        HttpResponse<byte[]> response = upload(
                oversizedContent, "image/png", EDITOR_ID, "EDITOR"
        );

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(new String(response.body(), StandardCharsets.UTF_8))
                .contains("MEDIA_SIZE_INVALID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_assets", Integer.class
        )).isZero();
    }

    private HttpResponse<byte[]> upload(
            byte[] fileContent, String mimeType, UUID actorId, String role
    ) throws Exception {
        String boundary = "stage41-" + UUID.randomUUID();
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"question.png\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8);
        HttpRequest request = authenticatedRequest(
                "/api/v1/admin/media/images", actorId, role
        ).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(prefix),
                        HttpRequest.BodyPublishers.ofByteArray(fileContent),
                        HttpRequest.BodyPublishers.ofByteArray(suffix)
                )).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpRequest.Builder authenticatedRequest(String path, UUID actorId, String role) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + path))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, actorId.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role);
    }
}
