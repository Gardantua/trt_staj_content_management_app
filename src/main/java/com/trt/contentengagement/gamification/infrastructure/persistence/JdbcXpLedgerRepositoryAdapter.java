package com.trt.contentengagement.gamification.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.XpLedgerRepository;
import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
import com.trt.contentengagement.gamification.application.XpLeaderboardResult;
import com.trt.contentengagement.gamification.application.XpSummary;
import com.trt.contentengagement.gamification.application.RankedXpEntry;
import com.trt.contentengagement.gamification.domain.XpPolicyVersion;
import com.trt.contentengagement.gamification.domain.XpReason;
import com.trt.contentengagement.gamification.domain.XpTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcXpLedgerRepositoryAdapter implements XpLedgerRepository, XpLeaderboardQuery {
    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, content_id, amount, reason, policy_version, reference_key,
                   source_attempt_id, related_transaction_id, created_by, note, occurred_at
            FROM xp_transactions
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcXpLedgerRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public XpTransaction appendIfAbsent(XpTransaction xpTransaction) {
        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version, reference_key,
                     source_attempt_id, related_transaction_id, created_by, note, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                xpTransaction.id(), xpTransaction.userId(), xpTransaction.contentId(),
                xpTransaction.amount(),
                xpTransaction.reason().name(), xpTransaction.policyVersion().name(),
                xpTransaction.referenceKey(), xpTransaction.sourceAttemptId(),
                xpTransaction.relatedTransactionId(), xpTransaction.createdBy(),
                xpTransaction.note(), java.sql.Timestamp.from(xpTransaction.occurredAt())
        );
        if (insertedRows == 1) {
            return requireByReferenceKey(xpTransaction.referenceKey());
        }
        Optional<XpTransaction> byReference = findByReferenceKey(xpTransaction.referenceKey());
        if (byReference.isPresent()) {
            return byReference.get();
        }
        if (xpTransaction.sourceAttemptId() != null) {
            return findBySourceAttemptId(xpTransaction.sourceAttemptId()).orElseThrow();
        }
        throw new IllegalStateException("XP transaction conflicted without a readable ledger row.");
    }

    @Override
    public Optional<XpTransaction> findById(UUID transactionId) {
        return single(SELECT_COLUMNS + " WHERE id = ?", transactionId);
    }

    @Override
    public Optional<XpTransaction> findBySourceAttemptId(UUID attemptId) {
        return single(SELECT_COLUMNS + " WHERE source_attempt_id = ?", attemptId);
    }

    @Override
    public List<XpTransaction> findAllForLeaderboardProjection() {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " ORDER BY occurred_at, id",
                this::mapTransaction
        );
    }

    @Override
    public XpSummary summarize(UUID userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(amount), 0) AS total_xp, COUNT(*) AS transaction_count
                FROM xp_transactions
                WHERE user_id = ?
                """,
                (resultSet, rowNumber) -> new XpSummary(
                        userId,
                        resultSet.getLong("total_xp"),
                        resultSet.getLong("transaction_count")
                ),
                userId
        );
    }

    @Override
    public XpLeaderboardResult findGlobal(UUID currentUserId, int limit) {
        return findLeaderboard(null, currentUserId, limit);
    }

    @Override
    public XpLeaderboardResult findByContent(
            UUID contentId, UUID currentUserId, int limit
    ) {
        return findLeaderboard(contentId, currentUserId, limit);
    }

    @Override
    public List<RankedXpEntry> findAllGlobal() {
        return findAllLeaderboardEntries(null);
    }

    @Override
    public List<UUID> findRankedContentIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT content_id FROM xp_transactions WHERE content_id IS NOT NULL",
                UUID.class
        );
    }

    @Override
    public List<RankedXpEntry> findAllByContent(UUID contentId) {
        return findAllLeaderboardEntries(contentId);
    }

    private XpLeaderboardResult findLeaderboard(
            UUID contentId, UUID currentUserId, int limit
    ) {
        String contentFilter = contentId == null ? "" : "WHERE content_id = ?";
        String sql = """
                WITH totals AS (
                    SELECT user_id,
                           SUM(amount)::bigint AS total_xp,
                           MIN(occurred_at) AS first_xp_at
                    FROM xp_transactions
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
                SELECT position, user_id, total_xp, first_xp_at, participant_count
                FROM ranked
                WHERE position <= ? OR user_id = ?
                ORDER BY position
                """.formatted(contentFilter);
        Object[] parameters = contentId == null
                ? new Object[]{limit, currentUserId}
                : new Object[]{contentId, limit, currentUserId};
        List<RankedRow> rows = jdbcTemplate.query(sql, this::mapRankedRow, parameters);
        List<RankedXpEntry> leaders = rows.stream()
                .filter(row -> row.entry().position() <= limit)
                .map(RankedRow::entry)
                .toList();
        RankedXpEntry currentUser = rows.stream()
                .map(RankedRow::entry)
                .filter(entry -> entry.userId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        long participantCount = rows.isEmpty() ? 0 : rows.getFirst().participantCount();
        return new XpLeaderboardResult(leaders, currentUser, participantCount);
    }

    private List<RankedXpEntry> findAllLeaderboardEntries(UUID contentId) {
        String contentFilter = contentId == null ? "" : "WHERE content_id = ?";
        String sql = """
                WITH totals AS (
                    SELECT user_id, SUM(amount)::bigint AS total_xp,
                           MIN(occurred_at) AS first_xp_at
                    FROM xp_transactions
                    %s
                    GROUP BY user_id
                )
                SELECT ROW_NUMBER() OVER (
                           ORDER BY total_xp DESC, first_xp_at ASC, user_id ASC
                       ) AS position,
                       user_id, total_xp, first_xp_at
                FROM totals
                ORDER BY position
                """.formatted(contentFilter);
        Object[] parameters = contentId == null ? new Object[]{} : new Object[]{contentId};
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new RankedXpEntry(
                resultSet.getLong("position"),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getLong("total_xp"),
                resultSet.getTimestamp("first_xp_at").toInstant()
        ), parameters);
    }

    private RankedRow mapRankedRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RankedRow(
                new RankedXpEntry(
                        resultSet.getLong("position"),
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getLong("total_xp"),
                        resultSet.getTimestamp("first_xp_at").toInstant()
                ),
                resultSet.getLong("participant_count")
        );
    }

    private Optional<XpTransaction> findByReferenceKey(String referenceKey) {
        return single(SELECT_COLUMNS + " WHERE reference_key = ?", referenceKey);
    }

    private XpTransaction requireByReferenceKey(String referenceKey) {
        return findByReferenceKey(referenceKey).orElseThrow();
    }

    private Optional<XpTransaction> single(String sql, Object parameter) {
        List<XpTransaction> transactions = jdbcTemplate.query(
                sql, this::mapTransaction, parameter
        );
        return transactions.stream().findFirst();
    }

    private XpTransaction mapTransaction(ResultSet resultSet, int rowNumber) throws SQLException {
        return new XpTransaction(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("content_id", UUID.class),
                resultSet.getInt("amount"),
                XpReason.valueOf(resultSet.getString("reason")),
                XpPolicyVersion.valueOf(resultSet.getString("policy_version")),
                resultSet.getString("reference_key"),
                resultSet.getObject("source_attempt_id", UUID.class),
                resultSet.getObject("related_transaction_id", UUID.class),
                resultSet.getObject("created_by", UUID.class),
                resultSet.getString("note"),
                resultSet.getTimestamp("occurred_at").toInstant()
        );
    }

    private record RankedRow(RankedXpEntry entry, long participantCount) { }
}
