package com.trt.contentengagement.messaging.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.messaging.application.InboxMessageRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInboxMessageRepository implements InboxMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcInboxMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claim(
            UUID eventId,
            String consumerName,
            String eventType,
            int eventVersion,
            Instant processedAt
    ) {
        return jdbcTemplate.update(
                """
                INSERT INTO inbox_messages
                    (event_id, consumer_name, event_type, event_version, received_at, processed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (consumer_name, event_id) DO NOTHING
                """,
                eventId, consumerName, eventType, eventVersion,
                Timestamp.from(processedAt), Timestamp.from(processedAt)
        ) == 1;
    }
}
