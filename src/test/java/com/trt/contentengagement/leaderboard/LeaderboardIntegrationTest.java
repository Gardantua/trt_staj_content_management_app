package com.trt.contentengagement.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
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
class LeaderboardIntegrationTest {
    private static final UUID CURRENT_USER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EARLIER_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HIGH_SCORE_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ADMIN_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("leaderboard_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final XpLeaderboardQuery xpLeaderboardQuery;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    LeaderboardIntegrationTest(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            XpLeaderboardQuery xpLeaderboardQuery
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.xpLeaderboardQuery = xpLeaderboardQuery;
    }

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM inbox_messages");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM xp_transactions");
        jdbcTemplate.update("DELETE FROM gameplay_answers");
        jdbcTemplate.update("DELETE FROM gameplay_quiz_reward_claims");
        jdbcTemplate.update("DELETE FROM gameplay_attempts");
        jdbcTemplate.update("DELETE FROM admin_audit_entries");
        jdbcTemplate.update("DELETE FROM quiz_questions");
        jdbcTemplate.update("DELETE FROM quiz_versions");
        jdbcTemplate.update("DELETE FROM quiz_definitions");
        jdbcTemplate.update("DELETE FROM catalog_episodes");
        jdbcTemplate.update("DELETE FROM catalog_seasons");
        jdbcTemplate.update("DELETE FROM catalog_contents");
        jdbcTemplate.update("DELETE FROM media_assets");
        jdbcTemplate.update("DELETE FROM identity_user_accounts");
    }

    @Test
    void globalLeaderboardUsesTotalXpAndReturnsCurrentUserOutsideTopN() throws Exception {
        createAccount(HIGH_SCORE_USER_ID, "Ada");
        createAccount(CURRENT_USER_ID, "Yunus");
        QuizFixture firstContent = createQuizFixture("First", true);
        QuizFixture secondContent = createQuizFixture("Second", true);
        award(firstContent, EARLIER_USER_ID, 100, Instant.parse("2026-08-04T10:00:00Z"));
        award(secondContent, EARLIER_USER_ID, 100, Instant.parse("2026-08-04T10:05:00Z"));
        award(firstContent, CURRENT_USER_ID, 200, Instant.parse("2026-08-04T10:01:00Z"));
        award(firstContent, HIGH_SCORE_USER_ID, 300, Instant.parse("2026-08-04T10:02:00Z"));

        JsonNode response = json(get("/api/v1/leaderboards/global?limit=1", CURRENT_USER_ID));

        assertThat(response.get("scope").stringValue()).isEqualTo("GLOBAL");
        assertThat(response.get("period").stringValue()).isEqualTo("ALL_TIME");
        assertThat(response.get("participantCount").longValue()).isEqualTo(3);
        assertThat(response.get("leaders").size()).isEqualTo(1);
        assertThat(response.get("leaders").get(0).get("userId").stringValue())
                .isEqualTo(HIGH_SCORE_USER_ID.toString());
        assertThat(response.get("leaders").get(0).get("displayName").stringValue())
                .isEqualTo("Ada");
        assertThat(response.get("currentUser").get("position").longValue()).isEqualTo(3);
        assertThat(response.get("currentUser").get("totalXp").longValue()).isEqualTo(200);
        assertThat(response.get("currentUser").get("displayName").stringValue())
                .isEqualTo("Yunus");
    }

    @Test
    void contentLeaderboardIncludesAdjustmentsAndExcludesOtherContent() throws Exception {
        QuizFixture firstContent = createQuizFixture("First", true);
        QuizFixture secondContent = createQuizFixture("Second", true);
        UUID original = award(
                firstContent, EARLIER_USER_ID, 100,
                Instant.parse("2026-08-04T10:00:00Z")
        );
        adjust(firstContent.contentId(), EARLIER_USER_ID, original, -40);
        award(firstContent, CURRENT_USER_ID, 70, Instant.parse("2026-08-04T10:01:00Z"));
        award(secondContent, EARLIER_USER_ID, 500, Instant.parse("2026-08-04T10:02:00Z"));

        JsonNode response = json(get(
                "/api/v1/leaderboards/contents/" + firstContent.contentId(), CURRENT_USER_ID
        ));

        assertThat(response.get("scope").stringValue()).isEqualTo("CONTENT");
        assertThat(response.get("contentId").stringValue())
                .isEqualTo(firstContent.contentId().toString());
        assertThat(response.get("leaders").get(0).get("userId").stringValue())
                .isEqualTo(CURRENT_USER_ID.toString());
        assertThat(response.get("leaders").get(0).get("totalXp").longValue()).isEqualTo(70);
        assertThat(response.get("leaders").get(1).get("totalXp").longValue()).isEqualTo(60);
    }

    @Test
    void equalXpUsesFirstParticipationThenUserIdAsDeterministicTieBreak() throws Exception {
        QuizFixture quiz = createQuizFixture("Tie", true);
        Instant sameTime = Instant.parse("2026-08-04T10:00:00Z");
        award(quiz, CURRENT_USER_ID, 100, sameTime.plusSeconds(1));
        award(quiz, HIGH_SCORE_USER_ID, 100, sameTime);
        award(quiz, EARLIER_USER_ID, 100, sameTime);

        JsonNode response = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));

        assertThat(response.get("leaders").get(0).get("userId").stringValue())
                .isEqualTo(EARLIER_USER_ID.toString());
        assertThat(response.get("leaders").get(1).get("userId").stringValue())
                .isEqualTo(HIGH_SCORE_USER_ID.toString());
        assertThat(response.get("leaders").get(2).get("userId").stringValue())
                .isEqualTo(CURRENT_USER_ID.toString());
    }

    @Test
    void leaderboardRequiresAuthenticationValidLimitAndPublishedContent() throws Exception {
        QuizFixture draftContent = createQuizFixture("Draft", false);

        assertThat(getWithoutAuthentication("/api/v1/leaderboards/global").statusCode())
                .isEqualTo(401);
        assertThat(get("/api/v1/leaderboards/global?limit=0", CURRENT_USER_ID).statusCode())
                .isEqualTo(400);
        HttpResponse<String> hiddenContent = get(
                "/api/v1/leaderboards/contents/" + draftContent.contentId(), CURRENT_USER_ID
        );
        assertThat(hiddenContent.statusCode()).isEqualTo(404);
        assertThat(hiddenContent.body()).contains("CONTENT_NOT_FOUND");
    }

    @Test
    void emptyLeaderboardDoesNotInventRankOrPercentile() throws Exception {
        JsonNode response = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));

        assertThat(response.get("participantCount").longValue()).isZero();
        assertThat(response.get("leaders").isEmpty()).isTrue();
        assertThat(response.get("currentUser").isNull()).isTrue();
        assertThat(response.has("percentile")).isFalse();
    }

    @Test
    void databaseRejectsXpAttributedToAContentDifferentFromItsAttempt() {
        QuizFixture actualQuiz = createQuizFixture("Actual", true);
        QuizFixture wrongContent = createQuizFixture("Wrong", true);
        UUID attemptId = UUID.randomUUID();
        insertAttempt(
                actualQuiz, CURRENT_USER_ID, attemptId, 100,
                Instant.parse("2026-08-04T10:00:00Z")
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version,
                     reference_key, source_attempt_id, occurred_at)
                VALUES (?, ?, ?, 100, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1', ?, ?, now())
                """,
                UUID.randomUUID(), CURRENT_USER_ID, wrongContent.contentId(),
                "QUIZ_ATTEMPT:" + attemptId, attemptId
        )).hasMessageContaining("does not match its source attempt");
    }

    @Test
    void contentTopNHasRecordedQueryPlanAndSubSecondP95OnSampleVolume() {
        QuizFixture quiz = createQuizFixture("Performance", true);
        insertSampleAwards(quiz, 2_000);
        List<Long> elapsedMicros = new ArrayList<>();

        for (int iteration = 0; iteration < 25; iteration++) {
            long startedAt = System.nanoTime();
            xpLeaderboardQuery.findByContent(quiz.contentId(), CURRENT_USER_ID, 20);
            elapsedMicros.add((System.nanoTime() - startedAt) / 1_000);
        }
        elapsedMicros.sort(Comparator.naturalOrder());
        long p95Micros = elapsedMicros.get(23);
        String queryPlan = String.join("\n", jdbcTemplate.queryForList(
                """
                EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
                SELECT user_id, SUM(amount), MIN(occurred_at)
                FROM xp_transactions
                WHERE content_id = ?
                GROUP BY user_id
                ORDER BY SUM(amount) DESC, MIN(occurred_at), user_id
                LIMIT 20
                """,
                String.class,
                quiz.contentId()
        ));

        System.out.printf("LEADERBOARD_SAMPLE users=2000 p95Micros=%d%n%s%n", p95Micros, queryPlan);
        assertThat(Duration.ofNanos(p95Micros * 1_000)).isLessThan(Duration.ofSeconds(1));
        assertThat(queryPlan).contains("xp_transactions").contains("GroupAggregate");
    }

    private QuizFixture createQuizFixture(String title, boolean published) {
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        String publicationStatus = published ? "PUBLISHED" : "DRAFT";
        jdbcTemplate.update(
                """
                INSERT INTO catalog_contents
                    (id, title, content_type, publication_status, created_at, updated_at)
                VALUES (?, ?, 'FILM', ?, now(), now())
                """,
                contentId, title, publicationStatus
        );
        jdbcTemplate.update(
                """
                INSERT INTO quiz_definitions (id, content_id, created_at, updated_at)
                VALUES (?, ?, now(), now())
                """,
                quizId, contentId
        );
        jdbcTemplate.update(
                """
                INSERT INTO quiz_versions
                    (id, quiz_id, version_number, title, status, scoring_policy_version,
                     created_at, published_at)
                VALUES (?, ?, 1, ?, 'PUBLISHED', 'STANDARD_V1', now(), now())
                """,
                versionId, quizId, title + " Quiz"
        );
        return new QuizFixture(contentId, quizId, versionId);
    }

    private void createAccount(UUID accountId, String displayName) {
        String email = accountId + "@example.test";
        jdbcTemplate.update(
                """
                INSERT INTO identity_user_accounts
                    (id, email, normalized_email, display_name, password_hash, role, created_at)
                VALUES (?, ?, ?, ?, 'test-password-hash', 'USER', now())
                """,
                accountId, email, email, displayName
        );
    }

    private UUID award(QuizFixture quiz, UUID userId, int amount, Instant occurredAt) {
        UUID attemptId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        insertAttempt(quiz, userId, attemptId, amount, occurredAt);
        jdbcTemplate.update(
                """
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version, reference_key,
                     source_attempt_id, occurred_at)
                VALUES (?, ?, ?, ?, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1', ?, ?, ?)
                """,
                transactionId, userId, quiz.contentId(), amount,
                "QUIZ_ATTEMPT:" + attemptId, attemptId,
                java.sql.Timestamp.from(occurredAt)
        );
        return transactionId;
    }

    private void adjust(
            UUID contentId, UUID userId, UUID relatedTransactionId, int amount
    ) {
        UUID adjustmentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version, reference_key,
                     related_transaction_id, created_by, note, occurred_at)
                VALUES (?, ?, ?, ?, 'ADMIN_ADJUSTMENT', 'ADMIN_ADJUSTMENT_V1', ?,
                        ?, ?, 'Verified correction', now())
                """,
                adjustmentId, userId, contentId, amount,
                "ADJUSTMENT:" + adjustmentId, relatedTransactionId, ADMIN_ID
        );
    }

    private void insertSampleAwards(QuizFixture quiz, int userCount) {
        List<Object[]> attempts = new ArrayList<>(userCount);
        List<Object[]> transactions = new ArrayList<>(userCount);
        Instant base = Instant.parse("2026-08-04T10:00:00Z");
        for (int index = 0; index < userCount; index++) {
            UUID userId = new UUID(0, index + 1L);
            UUID attemptId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Instant occurredAt = base.plusSeconds(index);
            attempts.add(attemptParameters(quiz, userId, attemptId, index % 500, occurredAt));
            transactions.add(new Object[]{
                    transactionId, userId, quiz.contentId(), index % 500,
                    "QUIZ_ATTEMPT:" + attemptId, attemptId,
                    java.sql.Timestamp.from(occurredAt)
            });
        }
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO gameplay_attempts
                    (id, user_id, quiz_id, quiz_version_id, scoring_policy_version,
                     started_at, deadline, status, score, completed_at)
                VALUES (?, ?, ?, ?, 'STANDARD_V1', ?, ?, 'COMPLETED', ?, ?)
                """,
                attempts
        );
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version, reference_key,
                     source_attempt_id, occurred_at)
                VALUES (?, ?, ?, ?, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1', ?, ?, ?)
                """,
                transactions
        );
    }

    private void insertAttempt(
            QuizFixture quiz, UUID userId, UUID attemptId, int score, Instant completedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO gameplay_attempts
                    (id, user_id, quiz_id, quiz_version_id, scoring_policy_version,
                     started_at, deadline, status, score, completed_at)
                VALUES (?, ?, ?, ?, 'STANDARD_V1', ?, ?, 'COMPLETED', ?, ?)
                """,
                attemptParameters(quiz, userId, attemptId, score, completedAt)
        );
    }

    private Object[] attemptParameters(
            QuizFixture quiz, UUID userId, UUID attemptId, int score, Instant completedAt
    ) {
        Instant startedAt = completedAt.minusSeconds(60);
        return new Object[]{
                attemptId, userId, quiz.quizId(), quiz.versionId(),
                java.sql.Timestamp.from(startedAt),
                java.sql.Timestamp.from(startedAt.plusSeconds(300)),
                score, java.sql.Timestamp.from(completedAt)
        };
    }

    private HttpResponse<String> get(String path, UUID actorId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER, actorId.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithoutAuthentication(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private record QuizFixture(UUID contentId, UUID quizId, UUID versionId) { }
}
