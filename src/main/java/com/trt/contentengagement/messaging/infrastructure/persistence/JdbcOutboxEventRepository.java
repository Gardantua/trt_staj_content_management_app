package com.trt.contentengagement.messaging.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.OutboxEventConflictException;
import com.trt.contentengagement.messaging.application.OutboxEventRepository;
import com.trt.contentengagement.messaging.application.QuizCompletedIntegrationEventV1;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JdbcOutboxEventRepository implements OutboxEventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOutboxEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void appendIfAbsent(OutboxEvent event) {
        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO outbox_events
                    (event_id, aggregate_type, aggregate_id, event_type, event_version,
                     payload, trace_id, trace_parent, trace_state, occurred_at, next_attempt_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId(), event.aggregateType(), event.aggregateId(), event.eventType(),
                event.eventVersion(), event.payload(), event.traceId(),
                event.traceParent(), event.traceState(),
                Timestamp.from(event.occurredAt()), Timestamp.from(event.occurredAt())
        );
        if (insertedRows == 0) {
            OutboxEvent stored = findById(event.eventId());
            if (!sameEvent(stored, event)) {
                throw new OutboxEventConflictException();
            }
        }
    }

    @Override
    public List<OutboxEvent> lockPendingBatch(Instant now, int batchSize) {
        return jdbcTemplate.query(
                """
                SELECT event_id, aggregate_type, aggregate_id, event_type, event_version,
                       payload::text, trace_id, trace_parent, trace_state,
                       occurred_at, publish_attempts
                FROM outbox_events
                WHERE published_at IS NULL AND next_attempt_at <= ?
                ORDER BY occurred_at, event_id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
                this::mapEvent,
                Timestamp.from(now), batchSize
        );
    }

    @Override
    public void markPublished(OutboxEvent event, Instant publishedAt) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET published_at = ?, publish_attempts = publish_attempts + 1, last_error = NULL
                WHERE event_id = ? AND published_at IS NULL
                """,
                Timestamp.from(publishedAt), event.eventId()
        );
    }

    @Override
    public void markFailed(OutboxEvent event, Instant nextAttemptAt, String errorMessage) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET publish_attempts = publish_attempts + 1,
                    next_attempt_at = ?,
                    last_error = ?
                WHERE event_id = ? AND published_at IS NULL
                """,
                Timestamp.from(nextAttemptAt), errorMessage, event.eventId()
        );
    }

    private OutboxEvent findById(UUID eventId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT event_id, aggregate_type, aggregate_id, event_type, event_version,
                       payload::text, trace_id, trace_parent, trace_state,
                       occurred_at, publish_attempts
                FROM outbox_events
                WHERE event_id = ?
                """,
                this::mapEvent,
                eventId
        );
    }

    private OutboxEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxEvent(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("aggregate_type"),
                resultSet.getObject("aggregate_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getInt("event_version"),
                resultSet.getString("payload"),
                resultSet.getString("trace_id"),
                resultSet.getString("trace_parent"),
                resultSet.getString("trace_state"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getInt("publish_attempts")
        );
    }

    private boolean sameEvent(OutboxEvent stored, OutboxEvent candidate) {
        boolean sameEnvelope = stored.aggregateType().equals(candidate.aggregateType())
                && stored.aggregateId().equals(candidate.aggregateId())
                && stored.eventType().equals(candidate.eventType())
                && stored.eventVersion() == candidate.eventVersion();
        if (!sameEnvelope) {
            return false;
        }
        if (QuizCompletedIntegrationEventV1.EVENT_TYPE.equals(stored.eventType())
                && stored.eventVersion() == QuizCompletedIntegrationEventV1.EVENT_VERSION) {
            return sameQuizCompletionPayload(stored.payload(), candidate.payload());
        }
        return objectMapper.readTree(stored.payload()).equals(objectMapper.readTree(candidate.payload()));
    }

    private boolean sameQuizCompletionPayload(String storedPayload, String candidatePayload) {
        try {
            QuizCompletedIntegrationEventV1 stored = objectMapper.readValue(
                    storedPayload, QuizCompletedIntegrationEventV1.class
            );
            QuizCompletedIntegrationEventV1 candidate = objectMapper.readValue(
                    candidatePayload, QuizCompletedIntegrationEventV1.class
            );
            return stored.eventId().equals(candidate.eventId())
                    && stored.attemptId().equals(candidate.attemptId())
                    && stored.userId().equals(candidate.userId())
                    && stored.quizId().equals(candidate.quizId())
                    && stored.quizVersionId().equals(candidate.quizVersionId())
                    && stored.finalScore() == candidate.finalScore()
                    && stored.xpPolicyVersion().equals(candidate.xpPolicyVersion());
        } catch (RuntimeException invalidPayload) {
            return false;
        }
    }
}
