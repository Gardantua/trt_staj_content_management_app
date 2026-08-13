package com.trt.contentengagement.leaderboardservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;
import com.trt.contentengagement.leaderboardservice.infrastructure.messaging.LeaderboardRabbitTopology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@SpringBootTest
class LeaderboardRabbitIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.5-alpine")
    ).withDatabaseName("leaderboard_rabbit_test")
            .withUsername("leaderboard")
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProjection() {
        jdbcTemplate.update("DELETE FROM leaderboard_xp_entries");
    }

    @Test
    void realRabbitDuplicateDeliveryCreatesOneProjectionRow() {
        UUID transactionId = UUID.randomUUID();
        XpChangedEventV1 event = new XpChangedEventV1(
                transactionId, transactionId, UUID.randomUUID(), UUID.randomUUID(),
                90, "QUIZ_COMPLETED", Instant.now()
        );
        String payload = objectMapper.writeValueAsString(event);

        rabbitTemplate.convertAndSend(
                LeaderboardRabbitTopology.EVENTS_EXCHANGE,
                LeaderboardRabbitTopology.XP_CHANGED_ROUTING_KEY,
                payload
        );
        rabbitTemplate.convertAndSend(
                LeaderboardRabbitTopology.EVENTS_EXCHANGE,
                LeaderboardRabbitTopology.XP_CHANGED_ROUTING_KEY,
                payload
        );

        awaitSingleRow();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_xp_entries", Integer.class
        )).isEqualTo(1);
    }

    private void awaitSingleRow() {
        Instant deadline = Instant.now().plusSeconds(20);
        while (Instant.now().isBefore(deadline)) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM leaderboard_xp_entries", Integer.class
            );
            if (count != null && count == 1) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Test wait was interrupted.", exception);
            }
        }
        throw new AssertionError("RabbitMQ event was not consumed before timeout.");
    }
}
