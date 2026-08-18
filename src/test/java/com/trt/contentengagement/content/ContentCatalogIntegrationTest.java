package com.trt.contentengagement.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
class ContentCatalogIntegrationTest {

    private static final UUID USER_ACTOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EDITOR_ACTOR_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("content_catalog_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int serverPort;

    @Autowired
    ContentCatalogIntegrationTest(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void clearCatalogData() {
        jdbcTemplate.update("DELETE FROM admin_audit_entries");
        jdbcTemplate.update("DELETE FROM catalog_episodes");
        jdbcTemplate.update("DELETE FROM catalog_seasons");
        jdbcTemplate.update("DELETE FROM catalog_contents");
        jdbcTemplate.update("DELETE FROM media_assets");
    }

    @Test
    void englishContentTranslationIsStoredAndSelectedByAcceptLanguage() throws Exception {
        HttpResponse<String> created = createFilm("ROCKY");
        String contentId = json(created).get("id").stringValue();
        attachCover(contentId);
        sendJson("POST", "/api/v1/admin/contents/" + contentId + "/publish", null, EDITOR_ACTOR_ID, "EDITOR");

        HttpResponse<String> saved = sendJson("PUT", "/api/v1/admin/contents/" + contentId + "/translations/en", """
                {"title":"ROCKY","description":"An amateur boxer gets a championship opportunity.",
                 "coverAlternativeText":"Rocky film cover","seasons":[]}
                """, EDITOR_ACTOR_ID, "EDITOR");
        assertThat(saved.statusCode()).isEqualTo(200);

        HttpResponse<String> english = sendGetWithLanguage(
                "/api/v1/contents/" + contentId, USER_ACTOR_ID, "USER", "en");
        assertThat(english.body()).contains("An amateur boxer gets a championship opportunity.")
                .contains("Rocky film cover");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_entries
                WHERE action = 'CONTENT_TRANSLATION_UPDATED' AND resource_id = ?::uuid
                """, Integer.class, contentId)).isEqualTo(1);
    }

    @Test
    void editorCanCreateMultipleSeasonsAndEpisodesWithOnePlan() throws Exception {
        HttpResponse<String> createdContent = sendJson(
                "POST",
                "/api/v1/admin/contents",
                """
                {"title":"Toplu Dizi","description":"Season plan","contentType":"SERIES"}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        String contentId = json(createdContent).get("id").stringValue();

        HttpResponse<String> plannedContent = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/season-plan",
                """
                {"seasons":[
                  {"seasonNumber":1,"episodeCount":29},
                  {"seasonNumber":2,"episodeCount":30}
                ]}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(plannedContent.statusCode()).isEqualTo(200);
        JsonNode seasons = json(plannedContent).get("seasons");
        assertThat(seasons.size()).isEqualTo(2);
        assertThat(seasons.get(0).get("title").stringValue()).isEqualTo("1. Sezon");
        assertThat(seasons.get(0).get("episodes").size()).isEqualTo(29);
        assertThat(seasons.get(0).get("episodes").get(28).get("title").stringValue())
                .isEqualTo("29. Bölüm");
        assertThat(seasons.get(1).get("episodes").size()).isEqualTo(30);

        Integer planAuditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM admin_audit_entries
                WHERE action = 'SEASON_PLAN_CREATED' AND resource_id = ?::uuid
                """,
                Integer.class,
                contentId
        );
        assertThat(planAuditCount).isEqualTo(1);
    }

    @Test
    void duplicateSeasonInPlanRollsBackTheWholeHierarchy() throws Exception {
        HttpResponse<String> createdContent = sendJson(
                "POST",
                "/api/v1/admin/contents",
                """
                {"title":"Atomic Dizi","description":"Rollback plan","contentType":"SERIES"}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        String contentId = json(createdContent).get("id").stringValue();

        HttpResponse<String> rejectedPlan = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/season-plan",
                """
                {"seasons":[
                  {"seasonNumber":1,"episodeCount":2},
                  {"seasonNumber":1,"episodeCount":3}
                ]}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(rejectedPlan.statusCode()).isEqualTo(409);
        assertThat(rejectedPlan.body()).contains("SEASON_NUMBER_DUPLICATE");
        Integer seasonCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalog_seasons WHERE content_id = ?::uuid",
                Integer.class,
                contentId
        );
        Integer episodeCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM catalog_episodes episode
                JOIN catalog_seasons season ON season.id = episode.season_id
                WHERE season.content_id = ?::uuid
                """,
                Integer.class,
                contentId
        );
        assertThat(seasonCount).isZero();
        assertThat(episodeCount).isZero();
    }

    @Test
    void editorCanAppendSeasonsAndEpisodesWithoutChangingPublishedHierarchy() throws Exception {
        HttpResponse<String> createdContent = sendJson(
                "POST", "/api/v1/admin/contents",
                """
                {"title":"Devam Eden Dizi","contentType":"SERIES"}
                """,
                EDITOR_ACTOR_ID, "EDITOR"
        );
        String contentId = json(createdContent).get("id").stringValue();
        JsonNode initialPlan = json(sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/season-plan",
                """
                {"seasons":[{"seasonNumber":1,"episodeCount":1}]}
                """,
                EDITOR_ACTOR_ID, "EDITOR"
        ));
        String firstSeasonId = initialPlan.get("seasons").get(0).get("id").stringValue();
        String firstEpisodeId = initialPlan.get("seasons").get(0)
                .get("episodes").get(0).get("id").stringValue();
        attachCover(contentId);
        sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/publish",
                null, EDITOR_ACTOR_ID, "EDITOR"
        );

        HttpResponse<String> appendedSeason = sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/season-plan",
                """
                {"seasons":[{"seasonNumber":2,"episodeCount":2}]}
                """,
                EDITOR_ACTOR_ID, "EDITOR"
        );
        HttpResponse<String> appendedEpisode = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/seasons/"
                        + firstSeasonId + "/episodes",
                """
                {"episodeNumber":2,"title":"2. Bölüm","description":null}
                """,
                EDITOR_ACTOR_ID, "EDITOR"
        );
        HttpResponse<String> rejectedPublishedEdit = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId + "/seasons/"
                        + firstSeasonId + "/episodes/" + firstEpisodeId,
                """
                {"episodeNumber":1,"title":"Değiştirilemez","description":null}
                """,
                EDITOR_ACTOR_ID, "EDITOR"
        );

        assertThat(appendedSeason.statusCode()).isEqualTo(200);
        assertThat(appendedEpisode.statusCode()).isEqualTo(201);
        assertThat(appendedEpisode.body())
                .contains(firstSeasonId)
                .contains(firstEpisodeId)
                .contains("2. Sezon")
                .contains("2. Bölüm");
        assertThat(rejectedPublishedEdit.statusCode()).isEqualTo(409);
        assertThat(rejectedPublishedEdit.body()).contains("PUBLISHED_CONTENT_IMMUTABLE");
        assertThat(sendGet(
                "/api/v1/contents/" + contentId, USER_ACTOR_ID, "USER"
        ).body()).contains("2. Sezon").contains("2. Bölüm");
    }

    @Test
    void editorCanBuildPublishAndAuditHierarchyThenUserCanReadIt() throws Exception {
        HttpResponse<String> createdContent = sendJson(
                "POST",
                "/api/v1/admin/contents",
                """
                {"title":"Gönül Dağı","description":"A family series","contentType":"SERIES"}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(createdContent.statusCode()).isEqualTo(201);
        String contentId = json(createdContent).get("id").stringValue();

        HttpResponse<String> createdSeason = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/seasons",
                "{\"seasonNumber\":1,\"title\":\"Birinci Sezon\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(createdSeason.statusCode()).isEqualTo(201);
        String seasonId = json(createdSeason).get("seasons").get(0).get("id").stringValue();

        HttpResponse<String> createdEpisode = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId + "/episodes",
                """
                {"episodeNumber":1,"title":"Başlangıç","description":"Pilot episode"}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(createdEpisode.statusCode()).isEqualTo(201);

        attachCover(contentId);

        HttpResponse<String> publishedContent = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/publish",
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(publishedContent.statusCode()).isEqualTo(200);
        assertThat(publishedContent.body()).contains("\"publicationStatus\":\"PUBLISHED\"");

        HttpResponse<String> publicDetails = sendGet(
                "/api/v1/contents/" + contentId,
                USER_ACTOR_ID,
                "USER"
        );
        assertThat(publicDetails.statusCode()).isEqualTo(200);
        assertThat(publicDetails.body())
                .contains("Gönül Dağı")
                .contains("Birinci Sezon")
                .contains("Başlangıç");

        HttpResponse<String> publicPage = sendGet(
                "/api/v1/contents?page=0&size=10",
                USER_ACTOR_ID,
                "USER"
        );
        assertThat(publicPage.statusCode()).isEqualTo(200);
        assertThat(publicPage.body())
                .contains("\"totalItems\":1")
                .contains(contentId);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE actor_id = ?",
                Integer.class,
                EDITOR_ACTOR_ID
        );
        assertThat(auditCount).isEqualTo(5);
    }

    @Test
    void draftContentIsHiddenFromUserEndpoints() throws Exception {
        HttpResponse<String> createdContent = createFilm("Draft Film");
        String contentId = json(createdContent).get("id").stringValue();

        HttpResponse<String> publicDetails = sendGet(
                "/api/v1/contents/" + contentId,
                USER_ACTOR_ID,
                "USER"
        );
        HttpResponse<String> publicPage = sendGet(
                "/api/v1/contents?page=0&size=20",
                USER_ACTOR_ID,
                "USER"
        );

        assertThat(publicDetails.statusCode()).isEqualTo(404);
        assertThat(publicDetails.body()).contains("\"code\":\"CONTENT_NOT_FOUND\"");
        assertThat(publicPage.body())
                .contains("\"totalItems\":0")
                .doesNotContain(contentId);
    }

    @Test
    void editorCanPageAllContentWhileNormalUserCannotUseAdminCatalog() throws Exception {
        HttpResponse<String> olderDraft = createFilm("Older Draft");
        String olderDraftId = json(olderDraft).get("id").stringValue();
        HttpResponse<String> newerDraft = createFilm("Newer Draft");
        String newerDraftId = json(newerDraft).get("id").stringValue();

        HttpResponse<String> firstPage = sendGet(
                "/api/v1/admin/contents?page=0&size=1",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        HttpResponse<String> secondPage = sendGet(
                "/api/v1/admin/contents?page=1&size=1",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        HttpResponse<String> forbiddenPage = sendGet(
                "/api/v1/admin/contents?page=0&size=20",
                USER_ACTOR_ID,
                "USER"
        );

        assertThat(firstPage.statusCode()).isEqualTo(200);
        assertThat(firstPage.body())
                .contains("\"totalItems\":2")
                .contains("\"totalPages\":2")
                .contains("\"publicationStatus\":\"DRAFT\"");
        assertThat(secondPage.statusCode()).isEqualTo(200);
        assertThat(firstPage.body() + secondPage.body())
                .contains(olderDraftId)
                .contains(newerDraftId);
        assertThat(forbiddenPage.statusCode()).isEqualTo(403);
        assertThat(forbiddenPage.body()).contains("\"code\":\"ACCESS_DENIED\"");
    }

    @Test
    void editorCanSearchContentTitlesAcrossThePaginatedAdminCatalog() throws Exception {
        createFilm("Alpha Belgeseli");
        createFilm("Başka Bir Film");
        createFilm("Alpha Dizisi");

        HttpResponse<String> firstSearchPage = sendGet(
                "/api/v1/admin/contents?query=ALPHA&page=0&size=1",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        HttpResponse<String> secondSearchPage = sendGet(
                "/api/v1/admin/contents?query=alpha&page=1&size=1",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        HttpResponse<String> trimmedSearch = sendGet(
                "/api/v1/admin/contents?query=%20Alpha%20&page=0&size=20",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(firstSearchPage.statusCode()).isEqualTo(200);
        assertThat(firstSearchPage.body())
                .contains("\"totalItems\":2")
                .contains("\"totalPages\":2")
                .doesNotContain("Başka Bir Film");
        assertThat(secondSearchPage.statusCode()).isEqualTo(200);
        assertThat(firstSearchPage.body() + secondSearchPage.body())
                .contains("Alpha Belgeseli")
                .contains("Alpha Dizisi");
        assertThat(trimmedSearch.statusCode()).isEqualTo(200);
        assertThat(trimmedSearch.body()).contains("\"totalItems\":2");
    }

    @Test
    void normalUserCannotManageContent() throws Exception {
        HttpResponse<String> response = sendJson(
                "POST",
                "/api/v1/admin/contents",
                "{\"title\":\"Forbidden\",\"contentType\":\"FILM\"}",
                USER_ACTOR_ID,
                "USER"
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalog_contents",
                Integer.class
        )).isZero();
    }

    @Test
    void validationAndPaginationLimitsReturnStableClientError() throws Exception {
        HttpResponse<String> invalidContent = sendJson(
                "POST",
                "/api/v1/admin/contents",
                "{\"title\":\" \",\"contentType\":\"FILM\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        HttpResponse<String> invalidPage = sendGet(
                "/api/v1/contents?page=-1&size=101",
                USER_ACTOR_ID,
                "USER"
        );
        HttpResponse<String> invalidAdminPage = sendGet(
                "/api/v1/admin/contents?page=-1&size=101",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(invalidContent.statusCode()).isEqualTo(400);
        assertThat(invalidContent.body()).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(invalidPage.statusCode()).isEqualTo(400);
        assertThat(invalidPage.body()).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(invalidAdminPage.statusCode()).isEqualTo(400);
        assertThat(invalidAdminPage.body()).contains("\"code\":\"VALIDATION_FAILED\"");
    }

    @Test
    void editorCanUpdateAndDeleteDraftHierarchy() throws Exception {
        HttpResponse<String> createdContent = sendJson(
                "POST",
                "/api/v1/admin/contents",
                "{\"title\":\"Old Series\",\"contentType\":\"SERIES\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        String contentId = json(createdContent).get("id").stringValue();
        HttpResponse<String> updatedContent = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId,
                "{\"title\":\"Updated Series\",\"description\":\"Updated\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(updatedContent.body()).contains("Updated Series");

        HttpResponse<String> createdSeason = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/seasons",
                "{\"seasonNumber\":1,\"title\":\"Season\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        String seasonId = json(createdSeason).get("seasons").get(0).get("id").stringValue();
        HttpResponse<String> createdEpisode = sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId + "/episodes",
                "{\"episodeNumber\":1,\"title\":\"Episode\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        String episodeId = json(createdEpisode)
                .get("seasons").get(0).get("episodes").get(0).get("id").stringValue();

        HttpResponse<String> updatedEpisode = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId
                        + "/episodes/" + episodeId,
                "{\"episodeNumber\":2,\"title\":\"Updated Episode\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(updatedEpisode.body())
                .contains("\"episodeNumber\":2")
                .contains("Updated Episode");

        assertThat(sendJson(
                "DELETE",
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId
                        + "/episodes/" + episodeId,
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(204);
        assertThat(sendJson(
                "DELETE",
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId,
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(204);
        assertThat(sendJson(
                "DELETE",
                "/api/v1/admin/contents/" + contentId,
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalog_contents",
                Integer.class
        )).isZero();
    }

    @Test
    void editorCanUpdateCoverAndHardDeletePublishedContent() throws Exception {
        HttpResponse<String> createdContent = createFilm("Published Film");
        String contentId = json(createdContent).get("id").stringValue();
        attachCover(contentId);
        assertThat(sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR"
        ).statusCode()).isEqualTo(200);

        HttpResponse<String> updatedContent = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId,
                "{\"title\":\"Published Film Updated\",\"description\":\"Updated after publish\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        UUID replacementCoverId = insertMediaAsset();
        HttpResponse<String> updatedCover = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId + "/cover",
                "{\"mediaAssetId\":\"" + replacementCoverId
                        + "\",\"alternativeText\":\"Updated cover\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(updatedContent.statusCode()).isEqualTo(200);
        assertThat(updatedContent.body()).contains("Published Film Updated");
        assertThat(updatedCover.statusCode()).isEqualTo(200);
        assertThat(updatedCover.body()).contains(replacementCoverId.toString());
        assertThat(sendJson(
                "DELETE", "/api/v1/admin/contents/" + contentId, null,
                EDITOR_ACTOR_ID, "EDITOR"
        ).statusCode()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalog_contents WHERE id = ?::uuid",
                Integer.class,
                contentId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE resource_id = ?::uuid",
                Integer.class,
                contentId
        )).isEqualTo(6);
    }

    @Test
    void postgresqlConstraintsRejectDuplicateHierarchyNumbers() {
        UUID contentId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        jdbcTemplate.update(
                """
                INSERT INTO catalog_contents
                    (id, title, content_type, publication_status, created_at, updated_at)
                VALUES (?, ?, 'SERIES', 'DRAFT', ?, ?)
                """,
                contentId,
                "Constraint Series",
                Timestamp.from(now),
                Timestamp.from(now)
        );
        jdbcTemplate.update(
                """
                INSERT INTO catalog_seasons (id, content_id, season_number, title)
                VALUES (?, ?, 1, 'Season')
                """,
                seasonId,
                contentId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO catalog_seasons (id, content_id, season_number, title)
                VALUES (?, ?, 1, 'Duplicate')
                """,
                UUID.randomUUID(),
                contentId
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                """
                INSERT INTO catalog_episodes (id, season_id, episode_number, title)
                VALUES (?, ?, 1, 'Episode')
                """,
                UUID.randomUUID(),
                seasonId
        );
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO catalog_episodes (id, season_id, episode_number, title)
                VALUES (?, ?, 1, 'Duplicate')
                """,
                UUID.randomUUID(),
                seasonId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private HttpResponse<String> createFilm(String title) throws IOException, InterruptedException {
        return sendJson(
                "POST",
                "/api/v1/admin/contents",
                "{\"title\":\"" + title + "\",\"contentType\":\"FILM\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
    }

    private void attachCover(String contentId) throws Exception {
        UUID mediaAssetId = insertMediaAsset();
        HttpResponse<String> response = sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId + "/cover",
                "{\"mediaAssetId\":\"" + mediaAssetId
                        + "\",\"alternativeText\":\"Icerik kapak gorseli\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private UUID insertMediaAsset() {
        UUID mediaAssetId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO media_assets
                    (id, storage_key, media_type, mime_type, byte_size, checksum_sha256,
                     width, height, created_by, created_at)
                VALUES (?, ?, 'IMAGE', 'image/png', 1, ?, 1, 1, ?, now())
                """,
                mediaAssetId,
                mediaAssetId + ".png",
                "0".repeat(64),
                EDITOR_ACTOR_ID
        );
        return mediaAssetId;
    }

    private HttpResponse<String> sendGet(String path, UUID actorId, String role)
            throws IOException, InterruptedException {
        return sendJson("GET", path, null, actorId, role);
    }

    private HttpResponse<String> sendGetWithLanguage(String path, UUID actorId, String role, String language)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + path))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, actorId.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role)
                .header("Accept-Language", language).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendJson(
            String method,
            String path,
            String requestBody,
            UUID actorId,
            String role
    ) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + path))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, actorId.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role);

        if (requestBody == null) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(requestBody));
        }
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return objectMapper.readTree(response.body());
    }
}
