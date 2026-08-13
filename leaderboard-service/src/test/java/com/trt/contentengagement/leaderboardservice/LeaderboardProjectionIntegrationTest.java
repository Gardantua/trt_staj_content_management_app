package com.trt.contentengagement.leaderboardservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.application.LeaderboardProjectionRepository;
import com.trt.contentengagement.leaderboardservice.application.LeaderboardQueryService;
import com.trt.contentengagement.leaderboardservice.application.LeaderboardRefreshService;
import com.trt.contentengagement.leaderboardservice.application.XpChangedEventHandler;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;
import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false",
        "app.leaderboard.redis-enabled=true",
        "app.leaderboard.refresh-enabled=false"
})
class LeaderboardProjectionIntegrationTest {
    private static final UUID CURRENT_USER =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FIRST_USER =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_USER =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CONTENT_ID = UUID.randomUUID();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.5-alpine")
    ).withDatabaseName("leaderboard_test")
            .withUsername("leaderboard")
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

    @Autowired
    private XpChangedEventHandler eventHandler;

    @Autowired
    private LeaderboardProjectionRepository projectionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LeaderboardRefreshService refreshService;

    @Autowired
    private LeaderboardQueryService queryService;

    @BeforeEach
    void clearProjection() {
        jdbcTemplate.update("DELETE FROM leaderboard_xp_entries");
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void duplicateEventIsStoredOnceAndTieBreakRemainsDeterministic() {
        Instant sameTime = Instant.parse("2026-08-12T08:00:00Z");
        XpChangedEventV1 first = event(FIRST_USER, 100, sameTime);
        XpChangedEventV1 second = event(SECOND_USER, 100, sameTime);
        XpChangedEventV1 current = event(CURRENT_USER, 80, sameTime.plusSeconds(1));

        assertThat(eventHandler.handle(first)).isTrue();
        assertThat(eventHandler.handle(first)).isFalse();
        eventHandler.handle(second);
        eventHandler.handle(current);

        LeaderboardResult result = projectionRepository.findGlobal(CURRENT_USER, 1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_xp_entries", Integer.class
        )).isEqualTo(3);
        assertThat(result.participantCount()).isEqualTo(3);
        assertThat(result.leaders()).hasSize(1);
        assertThat(result.leaders().getFirst().userId()).isEqualTo(FIRST_USER);
        assertThat(result.currentUser().position()).isEqualTo(3);
    }

    @Test
    void contentProjectionIncludesAdjustmentsAndExcludesOtherContent() {
        UUID otherContent = UUID.randomUUID();
        eventHandler.handle(event(FIRST_USER, 100, Instant.parse("2026-08-12T08:00:00Z")));
        eventHandler.handle(event(FIRST_USER, -40, Instant.parse("2026-08-12T08:01:00Z")));
        eventHandler.handle(event(CURRENT_USER, 70, Instant.parse("2026-08-12T08:02:00Z")));
        eventHandler.handle(event(
                SECOND_USER, otherContent, 500, "QUIZ_COMPLETED",
                Instant.parse("2026-08-12T08:03:00Z")
        ));

        LeaderboardResult result = projectionRepository.findByContent(
                CONTENT_ID, CURRENT_USER, 20
        );

        assertThat(result.leaders()).extracting(entry -> entry.totalXp())
                .containsExactly(70L, 60L);
    }

    @Test
    void redisProjectionIsRebuiltFromOwnedPostgresData() {
        eventHandler.handle(event(FIRST_USER, 100, Instant.parse("2026-08-12T08:00:00Z")));
        eventHandler.handle(event(CURRENT_USER, 70, Instant.parse("2026-08-12T08:01:00Z")));

        assertThat(refreshService.refresh()).isTrue();
        var view = queryService.global(CURRENT_USER, 1);

        assertThat(view.dataSource()).isEqualTo("REDIS");
        assertThat(view.result().leaders()).hasSize(1);
        assertThat(view.result().leaders().getFirst().userId()).isEqualTo(FIRST_USER);
        assertThat(view.result().currentUser().position()).isEqualTo(2);

        String firstGeneration = redisTemplate.opsForValue().get(
                "leaderboard-service:v1:active"
        );
        assertThat(refreshService.refresh()).isTrue();
        assertThat(redisTemplate.keys(
                "leaderboard-service:v1:" + firstGeneration + ":*"
        )).isEmpty();
    }

    private XpChangedEventV1 event(UUID userId, int amount, Instant occurredAt) {
        String reason = amount < 0 ? "ADMIN_ADJUSTMENT" : "QUIZ_COMPLETED";
        return event(userId, CONTENT_ID, amount, reason, occurredAt);
    }

    private XpChangedEventV1 event(
            UUID userId, UUID contentId, int amount, String reason, Instant occurredAt
    ) {
        UUID transactionId = UUID.randomUUID();
        return new XpChangedEventV1(
                transactionId, transactionId, userId, contentId,
                amount, reason, occurredAt
        );
    }
}
