package com.trt.contentengagement.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
import com.trt.contentengagement.leaderboard.application.LeaderboardProjectionStore;
import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import com.trt.contentengagement.leaderboard.application.LeaderboardProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.leaderboard.redis-enabled=true",
                "app.leaderboard.refresh-enabled=false"
        }
)
class RedisLeaderboardIntegrationTest {
    private static final UUID CURRENT_USER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FIRST_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ADMIN_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("redis_leaderboard_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:8.2-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final LeaderboardProjectionService projectionService;
    private final LeaderboardProjectionStore projectionStore;
    private final XpLeaderboardQuery xpLeaderboardQuery;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    RedisLeaderboardIntegrationTest(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            LeaderboardProjectionService projectionService,
            LeaderboardProjectionStore projectionStore,
            XpLeaderboardQuery xpLeaderboardQuery,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.projectionService = projectionService;
        this.projectionStore = projectionStore;
        this.xpLeaderboardQuery = xpLeaderboardQuery;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void clearState() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
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
    }

    @Test
    void rebuiltProjectionPreservesPostgresqlOrderAndCurrentUserOutsideTopN() throws Exception {
        QuizFixture quiz = createQuizFixture();
        Instant firstTime = Instant.parse("2026-08-04T10:00:00Z");
        award(quiz, FIRST_USER_ID, 100, firstTime);
        award(quiz, SECOND_USER_ID, 100, firstTime);
        award(quiz, CURRENT_USER_ID, 50, firstTime.plusSeconds(1));

        var rebuild = projectionService.rebuild();
        JsonNode global = json(get("/api/v1/leaderboards/global?limit=1", CURRENT_USER_ID));
        JsonNode content = json(get(
                "/api/v1/leaderboards/contents/" + quiz.contentId + "?limit=1",
                CURRENT_USER_ID
        ));

        assertThat(rebuild.replaced()).isTrue();
        assertThat(global.get("dataSource").stringValue()).isEqualTo("REDIS");
        assertThat(global.get("projectionGeneratedAt").isNull()).isFalse();
        assertThat(global.get("leaders").get(0).get("userId").stringValue())
                .isEqualTo(FIRST_USER_ID.toString());
        assertThat(global.get("currentUser").get("position").longValue()).isEqualTo(3);
        assertThat(content.get("dataSource").stringValue()).isEqualTo("REDIS");
        assertThat(projectionStore.findGlobal(CURRENT_USER_ID, 1).orElseThrow().result())
                .isEqualTo(xpLeaderboardQuery.findGlobal(CURRENT_USER_ID, 1));
    }

    @Test
    void missingProjectionFallsBackAndRepeatedRebuildDoesNotAccumulateGenerations()
            throws Exception {
        QuizFixture quiz = createQuizFixture();
        award(quiz, CURRENT_USER_ID, 70, Instant.parse("2026-08-04T10:00:00Z"));

        JsonNode beforeRebuild = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));
        projectionService.rebuild();
        projectionService.rebuild();
        JsonNode afterRebuild = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));
        var keys = redisTemplate.keys("leaderboard:v1:*");

        assertThat(beforeRebuild.get("dataSource").stringValue())
                .isEqualTo("POSTGRESQL_FALLBACK");
        assertThat(beforeRebuild.get("projectionGeneratedAt").isNull()).isTrue();
        assertThat(afterRebuild.get("dataSource").stringValue()).isEqualTo("REDIS");
        assertThat(keys).hasSizeLessThanOrEqualTo(11);
    }

    @Test
    void redisInterruptionFallsBackWithoutChangingPostgresqlXp() throws Exception {
        QuizFixture quiz = createQuizFixture();
        award(quiz, CURRENT_USER_ID, 90, Instant.parse("2026-08-04T10:00:00Z"));
        projectionService.rebuild();

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        JsonNode duringInterruption;
        try {
            duringInterruption = json(get(
                    "/api/v1/leaderboards/global", CURRENT_USER_ID
            ));
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
            waitForRedisRecovery();
        }

        assertThat(duringInterruption.get("dataSource").stringValue())
                .isEqualTo("POSTGRESQL_FALLBACK");
        assertThat(duringInterruption.get("leaders").get(0).get("totalXp").longValue())
                .isEqualTo(90);
        assertThat(xpLeaderboardQuery.findGlobal(CURRENT_USER_ID, 20)
                .leaders().getFirst().totalXp()).isEqualTo(90);
    }

    @Test
    void redisAndPostgresqlTopNPerformanceAreMeasuredOnSameSample() {
        QuizFixture quiz = createQuizFixture();
        insertSampleAwards(quiz, 2_000);
        projectionService.rebuild();
        List<Long> postgresqlMicros = new ArrayList<>();
        List<Long> redisMicros = new ArrayList<>();

        for (int iteration = 0; iteration < 25; iteration++) {
            long postgresqlStartedAt = System.nanoTime();
            xpLeaderboardQuery.findGlobal(CURRENT_USER_ID, 20);
            postgresqlMicros.add((System.nanoTime() - postgresqlStartedAt) / 1_000);

            long redisStartedAt = System.nanoTime();
            projectionStore.findGlobal(CURRENT_USER_ID, 20).orElseThrow();
            redisMicros.add((System.nanoTime() - redisStartedAt) / 1_000);
        }
        postgresqlMicros.sort(Comparator.naturalOrder());
        redisMicros.sort(Comparator.naturalOrder());
        long postgresqlP95 = postgresqlMicros.get(23);
        long redisP95 = redisMicros.get(23);

        System.out.printf(
                "LEADERBOARD_BEFORE_AFTER users=2000 postgresqlP95Micros=%d redisP95Micros=%d%n",
                postgresqlP95, redisP95
        );
        assertThat(postgresqlP95).isLessThan(1_000_000);
        assertThat(redisP95).isLessThan(1_000_000);
    }

    @Test
    void projectionIsExplicitlyStaleUntilControlledRebuildAndAdminOnlyCanRebuild()
            throws Exception {
        QuizFixture quiz = createQuizFixture();
        award(quiz, FIRST_USER_ID, 100, Instant.parse("2026-08-04T10:00:00Z"));
        projectionService.rebuild();
        award(quiz, CURRENT_USER_ID, 200, Instant.parse("2026-08-04T10:01:00Z"));

        JsonNode stale = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));
        HttpResponse<String> forbidden = postRebuild(CURRENT_USER_ID, "USER");
        HttpResponse<String> rebuilt = postRebuild(ADMIN_ID, "ADMIN");
        JsonNode fresh = json(get("/api/v1/leaderboards/global", CURRENT_USER_ID));

        assertThat(stale.get("currentUser").isNull()).isTrue();
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(rebuilt.statusCode()).isEqualTo(200);
        assertThat(fresh.get("leaders").get(0).get("userId").stringValue())
                .isEqualTo(CURRENT_USER_ID.toString());
    }

    private QuizFixture createQuizFixture() {
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO catalog_contents
                    (id, title, content_type, publication_status, created_at, updated_at)
                VALUES (?, 'Redis quiz', 'FILM', 'PUBLISHED', now(), now())
                """, contentId);
        jdbcTemplate.update("""
                INSERT INTO quiz_definitions (id, content_id, created_at, updated_at)
                VALUES (?, ?, now(), now())
                """, quizId, contentId);
        jdbcTemplate.update("""
                INSERT INTO quiz_versions
                    (id, quiz_id, version_number, title, status, scoring_policy_version,
                     created_at, published_at)
                VALUES (?, ?, 1, 'Redis quiz', 'PUBLISHED', 'STANDARD_V1', now(), now())
                """, versionId, quizId);
        return new QuizFixture(contentId, quizId, versionId);
    }

    private void award(QuizFixture quiz, UUID userId, int amount, Instant occurredAt) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO gameplay_attempts
                    (id, user_id, quiz_id, quiz_version_id, scoring_policy_version,
                     started_at, deadline, status, score, completed_at)
                VALUES (?, ?, ?, ?, 'STANDARD_V1', ?, ?, 'COMPLETED', ?, ?)
                """,
                attemptId, userId, quiz.quizId, quiz.versionId,
                java.sql.Timestamp.from(occurredAt.minusSeconds(60)),
                java.sql.Timestamp.from(occurredAt.plusSeconds(240)), amount,
                java.sql.Timestamp.from(occurredAt));
        jdbcTemplate.update("""
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version,
                     reference_key, source_attempt_id, occurred_at)
                VALUES (?, ?, ?, ?, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1', ?, ?, ?)
                """,
                UUID.randomUUID(), userId, quiz.contentId, amount,
                "QUIZ_ATTEMPT:" + attemptId, attemptId,
                java.sql.Timestamp.from(occurredAt));
    }

    private void insertSampleAwards(QuizFixture quiz, int userCount) {
        List<Object[]> attempts = new ArrayList<>(userCount);
        List<Object[]> transactions = new ArrayList<>(userCount);
        Instant base = Instant.parse("2026-08-04T10:00:00Z");
        for (int index = 0; index < userCount; index++) {
            UUID userId = new UUID(0, index + 1L);
            UUID attemptId = UUID.randomUUID();
            Instant occurredAt = base.plusSeconds(index);
            attempts.add(new Object[]{
                    attemptId, userId, quiz.quizId, quiz.versionId,
                    java.sql.Timestamp.from(occurredAt.minusSeconds(60)),
                    java.sql.Timestamp.from(occurredAt.plusSeconds(240)),
                    index % 500, java.sql.Timestamp.from(occurredAt)
            });
            transactions.add(new Object[]{
                    UUID.randomUUID(), userId, quiz.contentId, index % 500,
                    "QUIZ_ATTEMPT:" + attemptId, attemptId,
                    java.sql.Timestamp.from(occurredAt)
            });
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO gameplay_attempts
                    (id, user_id, quiz_id, quiz_version_id, scoring_policy_version,
                     started_at, deadline, status, score, completed_at)
                VALUES (?, ?, ?, ?, 'STANDARD_V1', ?, ?, 'COMPLETED', ?, ?)
                """, attempts);
        jdbcTemplate.batchUpdate("""
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version,
                     reference_key, source_attempt_id, occurred_at)
                VALUES (?, ?, ?, ?, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1', ?, ?, ?)
                """, transactions);
    }

    private void waitForRedisRecovery() throws InterruptedException {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                redisTemplate.hasKey("leaderboard:v1:active");
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                Thread.sleep(100);
            }
        }
        throw lastFailure;
    }

    private HttpResponse<String> get(String path, UUID actorId) throws Exception {
        return httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + path))
                        .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER,
                                actorId.toString())
                        .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, "USER")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postRebuild(UUID actorId, String role) throws Exception {
        return httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port
                                + "/api/v1/admin/leaderboards/rebuild"))
                        .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER,
                                actorId.toString())
                        .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER, role)
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private record QuizFixture(UUID contentId, UUID quizId, UUID versionId) {
    }
}
