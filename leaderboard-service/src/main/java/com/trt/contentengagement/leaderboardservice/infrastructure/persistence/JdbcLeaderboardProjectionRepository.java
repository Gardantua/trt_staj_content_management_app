package com.trt.contentengagement.leaderboardservice.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.application.LeaderboardProjectionRepository;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;
import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLeaderboardProjectionRepository implements LeaderboardProjectionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLeaderboardProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean appendIfAbsent(XpChangedEventV1 event, Instant consumedAt) {
        return jdbcTemplate.update(
                """
                INSERT INTO leaderboard_xp_entries
                    (event_id, transaction_id, user_id, content_id, amount, reason,
                     occurred_at, consumed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                event.eventId(), event.transactionId(), event.userId(), event.contentId(),
                event.amount(), event.reason(), Timestamp.from(event.occurredAt()),
                Timestamp.from(consumedAt)
        ) == 1;
    }

    @Override
    public LeaderboardResult findGlobal(UUID currentUserId, int limit) {
        return findLeaderboard(null, currentUserId, limit);
    }

    @Override
    public LeaderboardResult findByContent(UUID contentId, UUID currentUserId, int limit) {
        return findLeaderboard(contentId, currentUserId, limit);
    }

    @Override
    public List<LeaderboardEntry> findAllGlobal() {
        return findAll(null);
    }

    @Override
    public List<UUID> findContentIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT content_id FROM leaderboard_xp_entries ORDER BY content_id",
                UUID.class
        );
    }

    @Override
    public List<LeaderboardEntry> findAllByContent(UUID contentId) {
        return findAll(contentId);
    }

    private LeaderboardResult findLeaderboard(UUID contentId, UUID currentUserId, int limit) {
        String filter = contentId == null ? "" : "WHERE content_id = ?";
        String sql = rankedSql(filter) + """
                SELECT position, user_id, total_xp, first_xp_at, participant_count
                FROM ranked
                WHERE position <= ? OR user_id = ?
                ORDER BY position
                """;
        Object[] parameters = contentId == null
                ? new Object[]{limit, currentUserId}
                : new Object[]{contentId, limit, currentUserId};
        List<RankedRow> rows = jdbcTemplate.query(sql, this::mapRankedRow, parameters);
        List<LeaderboardEntry> leaders = rows.stream()
                .map(RankedRow::entry)
                .filter(entry -> entry.position() <= limit)
                .toList();
        LeaderboardEntry currentUser = rows.stream()
                .map(RankedRow::entry)
                .filter(entry -> entry.userId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        long participantCount = rows.isEmpty() ? 0 : rows.getFirst().participantCount();
        return new LeaderboardResult(leaders, currentUser, participantCount);
    }

    private List<LeaderboardEntry> findAll(UUID contentId) {
        String filter = contentId == null ? "" : "WHERE content_id = ?";
        String sql = rankedSql(filter) + """
                SELECT position, user_id, total_xp, first_xp_at
                FROM ranked ORDER BY position
                """;
        Object[] parameters = contentId == null ? new Object[]{} : new Object[]{contentId};
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> entry(resultSet), parameters);
    }

    private String rankedSql(String filter) {
        return """
                WITH totals AS (
                    SELECT user_id, SUM(amount)::bigint AS total_xp,
                           MIN(occurred_at) AS first_xp_at
                    FROM leaderboard_xp_entries
                    %s
                    GROUP BY user_id
                ), ranked AS (
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY total_xp DESC, first_xp_at ASC, user_id ASC
                           ) AS position,
                           user_id, total_xp, first_xp_at,
                           COUNT(*) OVER () AS participant_count
                    FROM totals
                )
                """.formatted(filter);
    }

    private RankedRow mapRankedRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RankedRow(entry(resultSet), resultSet.getLong("participant_count"));
    }

    private LeaderboardEntry entry(ResultSet resultSet) throws SQLException {
        return new LeaderboardEntry(
                resultSet.getLong("position"),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getLong("total_xp"),
                resultSet.getTimestamp("first_xp_at").toInstant()
        );
    }

    private record RankedRow(LeaderboardEntry entry, long participantCount) {
    }
}
