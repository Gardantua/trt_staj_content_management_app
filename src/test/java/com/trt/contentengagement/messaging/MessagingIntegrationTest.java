package com.trt.contentengagement.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;
import com.trt.contentengagement.gamification.application.XpService;
import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.OutboxEventRepository;
import com.trt.contentengagement.messaging.application.OutboxPublisher;
import com.trt.contentengagement.messaging.application.QuizCompletedIntegrationEventV1;
import com.trt.contentengagement.messaging.infrastructure.rabbit.RabbitMessagingTopology;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.messaging.rabbit-enabled=true",
        "app.messaging.publisher-delay=600000"
})
class MessagingIntegrationTest {
    private static final String TEST_TRACE_ID = "11111111111111111111111111111111";
    private static final String TEST_TRACE_PARENT =
            "00-" + TEST_TRACE_ID + "-2222222222222222-01";
    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("messaging_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.1-management-alpine")
    );

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    private final JdbcTemplate jdbcTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final XpService xpService;

    @Autowired
    MessagingIntegrationTest(
            JdbcTemplate jdbcTemplate,
            OutboxEventRepository outboxEventRepository,
            OutboxPublisher outboxPublisher,
            RabbitTemplate rabbitTemplate,
            RabbitAdmin rabbitAdmin,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            XpService xpService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisher = outboxPublisher;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.xpService = xpService;
    }

    @BeforeEach
    void clearState() {
        rabbitAdmin.purgeQueue(RabbitMessagingTopology.XP_QUEUE, true);
        rabbitAdmin.purgeQueue(RabbitMessagingTopology.XP_DEAD_LETTER_QUEUE, true);
        jdbcTemplate.update("DELETE FROM inbox_messages");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM xp_transactions");
        jdbcTemplate.update("DELETE FROM gameplay_answers");
        jdbcTemplate.update("DELETE FROM gameplay_quiz_reward_claims");
        jdbcTemplate.update("DELETE FROM gameplay_attempts");
        jdbcTemplate.update("DELETE FROM quiz_questions");
        jdbcTemplate.update("DELETE FROM quiz_versions");
        jdbcTemplate.update("DELETE FROM quiz_definitions");
        jdbcTemplate.update("DELETE FROM catalog_contents");
    }

    @AfterEach
    void ensureRabbitIsRunning() {
        if (RABBITMQ.isRunning()) {
            try {
                RABBITMQ.getDockerClient().unpauseContainerCmd(RABBITMQ.getContainerId()).exec();
            } catch (RuntimeException ignored) {
                // The container was not paused.
            }
        }
    }

    @Test
    void outboxPublishesToRabbitAndDuplicateDeliveryCreatesOneXpTransaction() {
        QuizCompletedIntegrationEventV1 event = completedAttemptFixture(200);
        appendOutbox(event);

        assertThat(outboxPublisher.publishPendingBatch()).isEqualTo(1);
        await(() -> count("xp_transactions") == 1 && count("inbox_messages") == 1);

        sendDirectly(event);
        await(() -> queueIsEmpty(RabbitMessagingTopology.XP_QUEUE));

        assertThat(count("xp_transactions")).isEqualTo(1);
        assertThat(count("inbox_messages")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount FROM xp_transactions WHERE source_attempt_id = ?",
                Integer.class,
                event.attemptId()
        )).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT published_at IS NOT NULL FROM outbox_events WHERE event_id = ?",
                Boolean.class,
                event.eventId()
        )).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = 'xp.changed'",
                Integer.class
        )).isEqualTo(1);
        String xpChangedPayload = jdbcTemplate.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE event_type = 'xp.changed'",
                String.class
        );
        assertThat(objectMapper.readTree(xpChangedPayload).get("amount").intValue())
                .isEqualTo(200);
        assertThat(xpService.replayLeaderboardEvents()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = 'xp.changed'",
                Integer.class
        )).isEqualTo(1);
        assertThat(meterRegistry.get("messaging.outbox.publish").timer().count())
                .isGreaterThanOrEqualTo(1);
        assertThat(meterRegistry.get("messaging.quiz.completed.consume").timer().count())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void brokerInterruptionLeavesOutboxPendingAndRecoveryPublishesIt() {
        QuizCompletedIntegrationEventV1 event = completedAttemptFixture(100);
        appendOutbox(event);

        RABBITMQ.getDockerClient().pauseContainerCmd(RABBITMQ.getContainerId()).exec();
        assertThat(outboxPublisher.publishPendingBatch()).isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT published_at IS NULL FROM outbox_events WHERE event_id = ?",
                Boolean.class,
                event.eventId()
        )).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT publish_attempts FROM outbox_events WHERE event_id = ?",
                Integer.class,
                event.eventId()
        )).isEqualTo(1);

        RABBITMQ.getDockerClient().unpauseContainerCmd(RABBITMQ.getContainerId()).exec();
        await(() -> {
            jdbcTemplate.update(
                    "UPDATE outbox_events SET next_attempt_at = now() WHERE event_id = ? AND published_at IS NULL",
                    event.eventId()
            );
            outboxPublisher.publishPendingBatch();
            Boolean isPublished = jdbcTemplate.queryForObject(
                    "SELECT published_at IS NOT NULL FROM outbox_events WHERE event_id = ?",
                    Boolean.class,
                    event.eventId()
            );
            return Boolean.TRUE.equals(isPublished);
        });
        await(() -> count("xp_transactions") == 1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount FROM xp_transactions WHERE source_attempt_id = ?",
                Integer.class,
                event.attemptId()
        )).isEqualTo(100);
    }

    @Test
    void legacyOutboxRowWithoutW3cTraceContextRemainsPublishable() {
        QuizCompletedIntegrationEventV1 event = completedAttemptFixture(75);
        outboxEventRepository.appendIfAbsent(new OutboxEvent(
                event.eventId(), "QUIZ_ATTEMPT", event.attemptId(),
                QuizCompletedIntegrationEventV1.EVENT_TYPE,
                QuizCompletedIntegrationEventV1.EVENT_VERSION,
                objectMapper.writeValueAsString(event),
                TEST_TRACE_ID,
                null,
                null,
                event.occurredAt(),
                0
        ));

        assertThat(outboxPublisher.publishPendingBatch()).isEqualTo(1);
        await(() -> count("xp_transactions") == 1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT published_at IS NOT NULL FROM outbox_events WHERE event_id = ?",
                Boolean.class,
                event.eventId()
        )).isTrue();
    }

    @Test
    void invalidEventIsRetriedAndRoutedToDeadLetterQueue() {
        rabbitTemplate.convertAndSend(
                RabbitMessagingTopology.EVENTS_EXCHANGE,
                RabbitMessagingTopology.QUIZ_COMPLETED_ROUTING_KEY,
                "{not-valid-json",
                message -> {
                    message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    return message;
                }
        );

        Message deadLetter = awaitMessage(RabbitMessagingTopology.XP_DEAD_LETTER_QUEUE);

        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{not-valid-json");
        assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
        assertThat(count("inbox_messages")).isZero();
        assertThat(count("xp_transactions")).isZero();
    }

    private QuizCompletedIntegrationEventV1 completedAttemptFixture(int finalScore) {
        Instant completedAt = Instant.now();
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID quizVersionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO catalog_contents
                    (id, title, content_type, publication_status, created_at, updated_at)
                VALUES (?, 'Messaging fixture', 'FILM', 'PUBLISHED', ?, ?)
                """,
                contentId, Timestamp.from(completedAt), Timestamp.from(completedAt)
        );
        jdbcTemplate.update(
                "INSERT INTO quiz_definitions (id, content_id, created_at, updated_at) VALUES (?, ?, ?, ?)",
                quizId, contentId, Timestamp.from(completedAt), Timestamp.from(completedAt)
        );
        jdbcTemplate.update(
                """
                INSERT INTO quiz_versions
                    (id, quiz_id, version_number, title, status, scoring_policy_version,
                     created_at, published_at)
                VALUES (?, ?, 1, 'Messaging quiz', 'PUBLISHED', 'STANDARD_V1', ?, ?)
                """,
                quizVersionId, quizId, Timestamp.from(completedAt), Timestamp.from(completedAt)
        );
        jdbcTemplate.update(
                """
                INSERT INTO gameplay_attempts
                    (id, user_id, quiz_id, quiz_version_id, scoring_policy_version,
                     timing_policy_version, started_at, deadline, status, score, completed_at)
                VALUES (?, ?, ?, ?, 'STANDARD_V1', 'STANDARD_V1', ?, ?, 'COMPLETED', ?, ?)
                """,
                attemptId, USER_ID, quizId, quizVersionId,
                Timestamp.from(completedAt.minusSeconds(60)),
                Timestamp.from(completedAt.plusSeconds(240)),
                finalScore, Timestamp.from(completedAt)
        );
        return QuizCompletedIntegrationEventV1.from(new QuizAttemptCompleted(
                attemptId, USER_ID, quizId, quizVersionId, finalScore, finalScore, completedAt
        ));
    }

    private void appendOutbox(QuizCompletedIntegrationEventV1 event) {
        outboxEventRepository.appendIfAbsent(new OutboxEvent(
                event.eventId(), "QUIZ_ATTEMPT", event.attemptId(),
                QuizCompletedIntegrationEventV1.EVENT_TYPE,
                QuizCompletedIntegrationEventV1.EVENT_VERSION,
                objectMapper.writeValueAsString(event),
                TEST_TRACE_ID,
                TEST_TRACE_PARENT,
                null,
                event.occurredAt(),
                0
        ));
    }

    private void sendDirectly(QuizCompletedIntegrationEventV1 event) {
        rabbitTemplate.convertAndSend(
                RabbitMessagingTopology.EVENTS_EXCHANGE,
                RabbitMessagingTopology.QUIZ_COMPLETED_ROUTING_KEY,
                objectMapper.writeValueAsString(event)
        );
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private boolean queueIsEmpty(String queueName) {
        var properties = rabbitAdmin.getQueueProperties(queueName);
        if (properties == null) {
            return false;
        }
        Object messageCount = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return messageCount instanceof Number number && number.intValue() == 0;
    }

    private void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plusSeconds(20);
        RuntimeException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
            sleepBriefly();
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AssertionError("Condition was not met before timeout.");
    }

    private Message awaitMessage(String queueName) {
        Instant deadline = Instant.now().plusSeconds(20);
        while (Instant.now().isBefore(deadline)) {
            Message message = rabbitTemplate.receive(queueName);
            if (message != null) {
                return message;
            }
            sleepBriefly();
        }
        throw new AssertionError("No message arrived in queue " + queueName);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test wait was interrupted.", exception);
        }
    }
}
