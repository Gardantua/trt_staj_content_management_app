package com.trt.contentengagement.quiz;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuizAuthoringIntegrationTest {

    private static final UUID USER_ACTOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EDITOR_ACTOR_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("quiz_authoring_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int serverPort;

    @Autowired
    QuizAuthoringIntegrationTest(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM admin_audit_entries");
        jdbcTemplate.update("DELETE FROM quiz_questions");
        jdbcTemplate.update("DELETE FROM quiz_versions");
        jdbcTemplate.update("DELETE FROM quiz_definitions");
        jdbcTemplate.update("DELETE FROM catalog_episodes");
        jdbcTemplate.update("DELETE FROM catalog_seasons");
        jdbcTemplate.update("DELETE FROM catalog_contents");
        jdbcTemplate.update("DELETE FROM media_assets");
    }

    @Test
    void editorCanAuthorPublishAndAuditQuizThenUserReadsSafeContract() throws Exception {
        String contentId = createContent("Gönül Dağı");
        QuizIdentifiers quiz = createQuiz(contentId, "Birinci Bölüm Quizi");

        HttpResponse<String> questionResponse = addValidQuestion(quiz);
        assertThat(questionResponse.statusCode()).isEqualTo(200);
        assertThat(questionResponse.body()).contains("\"correct\":true");

        HttpResponse<String> publishResponse = sendJson(
                "POST",
                adminVersionPath(quiz) + "/publish",
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(publishResponse.statusCode()).isEqualTo(200);
        assertThat(publishResponse.body()).contains("\"status\":\"PUBLISHED\"");

        HttpResponse<String> userResponse = sendGet(
                "/api/v1/quizzes/" + quiz.quizId(), USER_ACTOR_ID, "USER"
        );
        assertThat(userResponse.statusCode()).isEqualTo(200);
        assertThat(userResponse.body())
                .contains("Birinci Bölüm Quizi")
                .contains("Cirit")
                .doesNotContain("correct")
                .doesNotContain("isCorrect");

        HttpResponse<String> contentQuizList = sendGet(
                "/api/v1/contents/" + contentId + "/quizzes", USER_ACTOR_ID, "USER"
        );
        assertThat(contentQuizList.statusCode()).isEqualTo(200);
        assertThat(contentQuizList.body()).contains(quiz.quizId());

        HttpResponse<String> quizDiscoveryList = sendGet(
                "/api/v1/quizzes", USER_ACTOR_ID, "USER"
        );
        assertThat(quizDiscoveryList.statusCode()).isEqualTo(200);
        assertThat(quizDiscoveryList.body())
                .contains(quiz.quizId())
                .contains("\"questionCount\":1")
                .doesNotContain("answerOptions")
                .doesNotContain("correct");

        Integer auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM admin_audit_entries
                WHERE actor_id = ? AND action LIKE 'QUIZ_%'
                """,
                Integer.class,
                EDITOR_ACTOR_ID
        );
        assertThat(auditCount).isEqualTo(3);
    }

    @Test
    void informativeQuestionVisualAndAccessiblePromptReachUserWithoutCorrectAnswerFlag() throws Exception {
        String contentId = createContent("Dag Hikayesi");
        QuizIdentifiers quiz = createQuiz(contentId, "Dag Sorusu");
        UUID questionMediaId = insertMediaAsset();
        HttpResponse<String> questionResponse = sendJson(
                "POST",
                adminVersionPath(quiz) + "/questions",
                """
                {"questionOrder":1,"prompt":"Bu dagin adi nedir?","difficulty":"EASY",
                 "visualMediaId":"%s","visualRole":"INFORMATIVE",
                 "visualAlternativeText":"Karla kapli volkanik bir dag fotograifi",
                 "accessiblePrompt":"Fotografta karli, yuksek ve volkanik bir dag goruluyor. Bu dagin adi nedir?",
                 "answerOptions":[
                   {"optionOrder":1,"text":"Erciyes","correct":true},
                   {"optionOrder":2,"text":"Uludag","correct":false},
                   {"optionOrder":3,"text":"Agri","correct":false},
                   {"optionOrder":4,"text":"Toros","correct":false}
                 ]}
                """.formatted(questionMediaId),
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(questionResponse.statusCode()).isEqualTo(200);
        assertThat(sendJson(
                "POST", adminVersionPath(quiz) + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR"
        ).statusCode()).isEqualTo(200);

        HttpResponse<String> userResponse = sendGet(
                "/api/v1/quizzes/" + quiz.quizId(), USER_ACTOR_ID, "USER"
        );
        assertThat(userResponse.statusCode()).isEqualTo(200);
        assertThat(userResponse.body())
                .contains("\"role\":\"INFORMATIVE\"")
                .contains("\"mediaAssetId\":\"" + questionMediaId + "\"")
                .contains("Fotografta karli, yuksek ve volkanik bir dag goruluyor")
                .doesNotContain("\"correct\"");
    }

    @Test
    void incompleteDraftIsHiddenAndCannotBePublished() throws Exception {
        String contentId = createContent("Teşkilat");
        QuizIdentifiers quiz = createQuiz(contentId, "Eksik Quiz");

        HttpResponse<String> publicResponse = sendGet(
                "/api/v1/quizzes/" + quiz.quizId(), USER_ACTOR_ID, "USER"
        );
        HttpResponse<String> publishResponse = sendJson(
                "POST", adminVersionPath(quiz) + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR"
        );

        assertThat(publicResponse.statusCode()).isEqualTo(404);
        assertThat(publicResponse.body()).contains("\"code\":\"QUIZ_NOT_FOUND\"");
        assertThat(publishResponse.statusCode()).isEqualTo(409);
        assertThat(publishResponse.body()).contains("\"code\":\"QUIZ_REQUIRES_QUESTION\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE action = 'QUIZ_VERSION_PUBLISHED'",
                Integer.class
        )).isZero();
    }

    @Test
    void normalUserCannotUseQuizManagementApi() throws Exception {
        String contentId = createContent("Seksenler");

        HttpResponse<String> response = sendJson(
                "POST",
                "/api/v1/admin/quizzes",
                "{\"contentId\":\"" + contentId + "\",\"title\":\"Yasak Quiz\"}",
                USER_ACTOR_ID,
                "USER"
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_definitions", Integer.class
        )).isZero();
    }

    @Test
    void publishedContentWithoutQuizReturnsEmptyAdminList() throws Exception {
        String contentId = createContent("Quizsiz Film");

        HttpResponse<String> response = sendGet(
                "/api/v1/admin/quizzes?contentId=" + contentId,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void editorCanPermanentlyDeleteQuizWithoutGameplayHistory() throws Exception {
        String contentId = createContent("Silinebilir Film");
        QuizIdentifiers quiz = createQuiz(contentId, "Silinebilir Quiz");

        HttpResponse<String> deleteResponse = sendJson(
                "DELETE",
                "/api/v1/admin/quizzes/" + quiz.quizId(),
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(deleteResponse.statusCode()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_definitions WHERE id = ?",
                Integer.class,
                UUID.fromString(quiz.quizId())
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE action = 'QUIZ_DELETED'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void previouslyPublishedQuizMovesToHistoryAndCannotBeHardDeleted() throws Exception {
        String contentId = createContent("Geçmiş Filmi");
        UUID coverMediaId = insertMediaAsset();
        sendJson(
                "PUT", "/api/v1/admin/contents/" + contentId + "/cover",
                "{\"mediaAssetId\":\"" + coverMediaId
                        + "\",\"alternativeText\":\"Film kapağı\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
        sendJson("POST", "/api/v1/admin/contents/" + contentId + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR");
        QuizIdentifiers quiz = createQuiz(contentId, "Geçmişe Taşınacak Quiz");
        addValidQuestion(quiz);
        sendJson("POST", adminVersionPath(quiz) + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR");
        sendJson("POST", "/api/v1/admin/quizzes/" + quiz.quizId() + "/versions", null,
                EDITOR_ACTOR_ID, "EDITOR");

        HttpResponse<String> retireResponse = sendJson(
                "POST", "/api/v1/admin/quizzes/" + quiz.quizId() + "/retire", null,
                EDITOR_ACTOR_ID, "EDITOR"
        );
        HttpResponse<String> deleteResponse = sendJson(
                "DELETE", "/api/v1/admin/quizzes/" + quiz.quizId(), null,
                EDITOR_ACTOR_ID, "EDITOR"
        );

        assertThat(retireResponse.statusCode()).isEqualTo(200);
        assertThat(json(retireResponse).get("versions")).hasSize(1);
        assertThat(json(retireResponse).get("versions").get(0).get("status").stringValue())
                .isEqualTo("ARCHIVED");
        assertThat(deleteResponse.statusCode()).isEqualTo(409);
        assertThat(json(deleteResponse).get("code").stringValue())
                .isEqualTo("QUIZ_DELETE_REQUIRES_RETIREMENT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE action = 'QUIZ_RETIRED'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void publishedVersionIsImmutableAndNewDraftBecomesNextPublishedVersion() throws Exception {
        String contentId = createContent("Al Sancak");
        QuizIdentifiers quiz = createQuiz(contentId, "Sürüm Bir");
        addValidQuestion(quiz);
        assertThat(sendJson(
                "POST", adminVersionPath(quiz) + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR"
        ).statusCode()).isEqualTo(200);

        HttpResponse<String> inPlaceUpdate = sendJson(
                "PUT",
                adminVersionPath(quiz),
                "{\"title\":\"Yerinde Değişiklik\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(inPlaceUpdate.statusCode()).isEqualTo(409);
        assertThat(inPlaceUpdate.body()).contains("QUIZ_VERSION_IMMUTABLE");

        HttpResponse<String> draftResponse = sendJson(
                "POST",
                "/api/v1/admin/quizzes/" + quiz.quizId() + "/versions",
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(draftResponse.statusCode()).isEqualTo(200);
        JsonNode secondVersion = json(draftResponse).get("versions").get(1);
        String secondVersionId = secondVersion.get("id").stringValue();
        assertThat(secondVersion.get("versionNumber").intValue()).isEqualTo(2);
        assertThat(secondVersion.get("status").stringValue()).isEqualTo("DRAFT");

        assertThat(sendJson(
                "PUT",
                "/api/v1/admin/quizzes/" + quiz.quizId() + "/versions/" + secondVersionId,
                "{\"title\":\"Sürüm İki\",\"description\":\"Yeni sürüm\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(200);
        assertThat(sendJson(
                "POST",
                "/api/v1/admin/quizzes/" + quiz.quizId() + "/versions/"
                        + secondVersionId + "/publish",
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(200);

        HttpResponse<String> publicResponse = sendGet(
                "/api/v1/quizzes/" + quiz.quizId(), USER_ACTOR_ID, "USER"
        );
        assertThat(publicResponse.body())
                .contains("\"versionNumber\":2")
                .contains("Sürüm İki");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM quiz_versions WHERE id = ?::uuid",
                String.class,
                quiz.versionId()
        )).isEqualTo("ARCHIVED");
    }

    @Test
    void multipleCorrectOptionsAreRejectedWithoutPartialWrite() throws Exception {
        String contentId = createContent("Kudüs Fatihi");
        QuizIdentifiers quiz = createQuiz(contentId, "Doğru Cevap Kuralı");

        HttpResponse<String> response = sendJson(
                "POST",
                adminVersionPath(quiz) + "/questions",
                """
                {"questionOrder":1,"prompt":"Soru","difficulty":"EASY","answerOptions":[
                  {"optionOrder":1,"text":"A","correct":true},
                  {"optionOrder":2,"text":"B","correct":true},
                  {"optionOrder":3,"text":"C","correct":false},
                  {"optionOrder":4,"text":"D","correct":false}
                ]}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("QUIZ_MULTIPLE_CORRECT_OPTIONS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions", Integer.class
        )).isZero();
    }

    @Test
    void editorCanChangeCorrectOptionAndDeleteQuestionInsideDraft() throws Exception {
        String contentId = createContent("Mehmed Fetihler Sultanı");
        QuizIdentifiers quiz = createQuiz(contentId, "Düzenlenen Quiz");
        HttpResponse<String> createdQuestion = addValidQuestion(quiz);
        String questionId = json(createdQuestion)
                .get("versions").get(0).get("questions").get(0).get("id").stringValue();

        HttpResponse<String> updateResponse = sendJson(
                "PUT",
                adminVersionPath(quiz) + "/questions/" + questionId,
                """
                {"questionOrder":1,"prompt":"Güncellenen soru","difficulty":"MEDIUM",
                 "answerOptions":[
                   {"optionOrder":1,"text":"Cirit","correct":false},
                   {"optionOrder":2,"text":"Buz hokeyi","correct":true},
                   {"optionOrder":3,"text":"Futbol","correct":false},
                   {"optionOrder":4,"text":"Tenis","correct":false}
                 ]}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.body())
                .contains("Güncellenen soru")
                .contains("\"optionOrder\":2,\"text\":\"Buz hokeyi\",\"correct\":true");

        HttpResponse<String> deleteResponse = sendJson(
                "DELETE",
                adminVersionPath(quiz) + "/questions/" + questionId,
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(200);
        assertThat(deleteResponse.body()).contains("\"questions\":[]");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions", Integer.class
        )).isZero();
    }

    @Test
    void archivedVersionIsNoLongerVisibleToUser() throws Exception {
        String contentId = createContent("Barbaroslar");
        QuizIdentifiers quiz = createQuiz(contentId, "Arşivlenen Quiz");
        addValidQuestion(quiz);
        assertThat(sendJson(
                "POST", adminVersionPath(quiz) + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR"
        ).statusCode()).isEqualTo(200);

        HttpResponse<String> archiveResponse = sendJson(
                "POST", adminVersionPath(quiz) + "/archive", null,
                EDITOR_ACTOR_ID, "EDITOR"
        );
        HttpResponse<String> publicResponse = sendGet(
                "/api/v1/quizzes/" + quiz.quizId(), USER_ACTOR_ID, "USER"
        );

        assertThat(archiveResponse.statusCode()).isEqualTo(200);
        assertThat(archiveResponse.body()).contains("\"status\":\"ARCHIVED\"");
        assertThat(publicResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void editorCanCreateContentSeasonAndEpisodeScopedQuizzes() throws Exception {
        SeriesFixture series = createSeriesContent("Kasaba Hikayesi");

        HttpResponse<String> contentQuiz = createScopedQuiz(
                series.contentId(), "CONTENT", null, null, "Dizi Geneli"
        );
        HttpResponse<String> seasonQuiz = createScopedQuiz(
                series.contentId(), "SEASON", series.seasonId(), null, "Sezon Quizi"
        );
        HttpResponse<String> episodeQuiz = createScopedQuiz(
                series.contentId(), "EPISODE", series.seasonId(), series.episodeId(),
                "Bölüm Quizi"
        );

        assertThat(contentQuiz.statusCode()).isEqualTo(201);
        assertThat(contentQuiz.body()).contains("\"scopeType\":\"CONTENT\"");
        assertThat(seasonQuiz.statusCode()).isEqualTo(201);
        assertThat(seasonQuiz.body())
                .contains("\"scopeType\":\"SEASON\"")
                .contains("\"seasonId\":\"" + series.seasonId() + "\"");
        assertThat(episodeQuiz.statusCode()).isEqualTo(201);
        assertThat(episodeQuiz.body())
                .contains("\"scopeType\":\"EPISODE\"")
                .contains("\"episodeId\":\"" + series.episodeId() + "\"");

        HttpResponse<String> listResponse = sendGet(
                "/api/v1/admin/quizzes?contentId=" + series.contentId(),
                EDITOR_ACTOR_ID, "EDITOR"
        );
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body())
                .contains("Dizi Geneli", "Sezon Quizi", "Bölüm Quizi")
                .contains("\"questionCount\":0");
    }

    @Test
    void quizScopeCannotReferenceAnotherContentsSeason() throws Exception {
        SeriesFixture firstSeries = createSeriesContent("Birinci Dizi");
        SeriesFixture secondSeries = createSeriesContent("İkinci Dizi");

        HttpResponse<String> response = createScopedQuiz(
                firstSeries.contentId(), "EPISODE", secondSeries.seasonId(),
                secondSeries.episodeId(), "Yanlış Bağlantı"
        );

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_definitions WHERE content_id = ?::uuid",
                Integer.class, firstSeries.contentId()
        )).isZero();
    }

    @Test
    void postgresqlConstraintsRejectDuplicateOrdersAndCorrectOptions() {
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-04T08:00:00Z");
        jdbcTemplate.update(
                """
                INSERT INTO catalog_contents
                    (id, title, content_type, publication_status, created_at, updated_at)
                VALUES (?, 'Constraint Content', 'FILM', 'DRAFT', ?, ?)
                """,
                contentId,
                Timestamp.from(now),
                Timestamp.from(now)
        );
        jdbcTemplate.update(
                "INSERT INTO quiz_definitions (id, content_id, created_at, updated_at) VALUES (?, ?, ?, ?)",
                quizId, contentId, Timestamp.from(now), Timestamp.from(now)
        );
        jdbcTemplate.update(
                """
                INSERT INTO quiz_versions
                    (id, quiz_id, version_number, title, status, scoring_policy_version, created_at)
                VALUES (?, ?, 1, 'Draft', 'DRAFT', 'STANDARD_V1', ?)
                """,
                versionId, quizId, Timestamp.from(now)
        );
        transactionTemplate.executeWithoutResult(ignored -> {
            jdbcTemplate.update(
                    """
                    INSERT INTO quiz_questions
                        (id, quiz_version_id, question_order, prompt, difficulty)
                    VALUES (?, ?, 1, 'Question', 'MEDIUM')
                    """,
                    questionId, versionId
            );
            for (int optionOrder = 1; optionOrder <= 4; optionOrder++) {
                jdbcTemplate.update(
                        """
                        INSERT INTO quiz_answer_options
                            (id, question_id, option_order, option_text, is_correct)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(), questionId, optionOrder,
                        "Option " + optionOrder, optionOrder == 1
                );
            }
        });

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO quiz_questions
                    (id, quiz_version_id, question_order, prompt, difficulty)
                VALUES (?, ?, 1, 'Duplicate', 'MEDIUM')
                """,
                UUID.randomUUID(), versionId
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO quiz_answer_options
                    (id, question_id, option_order, option_text, is_correct)
                VALUES (?, ?, 5, 'B', true)
                """,
                UUID.randomUUID(), questionId
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(ignored -> {
            UUID incompleteQuestionId = UUID.randomUUID();
            jdbcTemplate.update(
                    """
                    INSERT INTO quiz_questions
                        (id, quiz_version_id, question_order, prompt, difficulty)
                    VALUES (?, ?, 2, 'Incomplete question', 'MEDIUM')
                    """,
                    incompleteQuestionId, versionId
            );
            for (int optionOrder = 1; optionOrder <= 3; optionOrder++) {
                jdbcTemplate.update(
                        """
                        INSERT INTO quiz_answer_options
                            (id, question_id, option_order, option_text, is_correct)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(), incompleteQuestionId, optionOrder,
                        "Option " + optionOrder, optionOrder == 1
                );
            }
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    private String createContent(String title) throws Exception {
        HttpResponse<String> response = sendJson(
                "POST",
                "/api/v1/admin/contents",
                "{\"title\":\"" + title + "\",\"contentType\":\"FILM\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(response.statusCode()).isEqualTo(201);
        String contentId = json(response).get("id").stringValue();
        UUID mediaAssetId = insertMediaAsset();
        assertThat(sendJson(
                "PUT",
                "/api/v1/admin/contents/" + contentId + "/cover",
                "{\"mediaAssetId\":\"" + mediaAssetId
                        + "\",\"alternativeText\":\"Icerik kapak gorseli\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(200);
        assertThat(sendJson(
                "POST",
                "/api/v1/admin/contents/" + contentId + "/publish",
                null,
                EDITOR_ACTOR_ID,
                "EDITOR"
        ).statusCode()).isEqualTo(200);
        return contentId;
    }

    private SeriesFixture createSeriesContent(String title) throws Exception {
        HttpResponse<String> contentResponse = sendJson(
                "POST", "/api/v1/admin/contents",
                "{\"title\":\"" + title + "\",\"contentType\":\"SERIES\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
        String contentId = json(contentResponse).get("id").stringValue();
        HttpResponse<String> seasonResponse = sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/seasons",
                "{\"seasonNumber\":1,\"title\":\"Birinci Sezon\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
        String seasonId = json(seasonResponse).get("seasons").get(0).get("id").stringValue();
        HttpResponse<String> episodeResponse = sendJson(
                "POST", "/api/v1/admin/contents/" + contentId + "/seasons/"
                        + seasonId + "/episodes",
                "{\"episodeNumber\":1,\"title\":\"İlk Bölüm\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
        String episodeId = json(episodeResponse).get("seasons").get(0)
                .get("episodes").get(0).get("id").stringValue();
        UUID mediaAssetId = insertMediaAsset();
        sendJson(
                "PUT", "/api/v1/admin/contents/" + contentId + "/cover",
                "{\"mediaAssetId\":\"" + mediaAssetId
                        + "\",\"alternativeText\":\"Dizi kapak görseli\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
        sendJson("POST", "/api/v1/admin/contents/" + contentId + "/publish", null,
                EDITOR_ACTOR_ID, "EDITOR");
        return new SeriesFixture(contentId, seasonId, episodeId);
    }

    private HttpResponse<String> createScopedQuiz(
            String contentId, String scopeType, String seasonId,
            String episodeId, String title
    ) throws Exception {
        String scopeFields = "\"scopeType\":\"" + scopeType + "\""
                + (seasonId == null ? "" : ",\"seasonId\":\"" + seasonId + "\"")
                + (episodeId == null ? "" : ",\"episodeId\":\"" + episodeId + "\"");
        return sendJson(
                "POST", "/api/v1/admin/quizzes",
                "{\"contentId\":\"" + contentId + "\"," + scopeFields
                        + ",\"title\":\"" + title + "\"}",
                EDITOR_ACTOR_ID, "EDITOR"
        );
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

    private QuizIdentifiers createQuiz(String contentId, String title) throws Exception {
        HttpResponse<String> response = sendJson(
                "POST",
                "/api/v1/admin/quizzes",
                "{\"contentId\":\"" + contentId + "\",\"title\":\"" + title + "\"}",
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode responseJson = json(response);
        return new QuizIdentifiers(
                responseJson.get("id").stringValue(),
                responseJson.get("versions").get(0).get("id").stringValue()
        );
    }

    private HttpResponse<String> addValidQuestion(QuizIdentifiers quiz) throws Exception {
        return sendJson(
                "POST",
                adminVersionPath(quiz) + "/questions",
                """
                {"questionOrder":1,"prompt":"Geleneksel spor hangisidir?","difficulty":"EASY",
                 "answerOptions":[
                   {"optionOrder":1,"text":"Cirit","correct":true},
                   {"optionOrder":2,"text":"Buz hokeyi","correct":false},
                   {"optionOrder":3,"text":"Futbol","correct":false},
                   {"optionOrder":4,"text":"Tenis","correct":false}
                 ]}
                """,
                EDITOR_ACTOR_ID,
                "EDITOR"
        );
    }

    private String adminVersionPath(QuizIdentifiers quiz) {
        return "/api/v1/admin/quizzes/" + quiz.quizId()
                + "/versions/" + quiz.versionId();
    }

    private HttpResponse<String> sendGet(String path, UUID actorId, String role)
            throws IOException, InterruptedException {
        return sendJson("GET", path, null, actorId, role);
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

    private record QuizIdentifiers(String quizId, String versionId) {
    }

    private record SeriesFixture(String contentId, String seasonId, String episodeId) {
    }
}
