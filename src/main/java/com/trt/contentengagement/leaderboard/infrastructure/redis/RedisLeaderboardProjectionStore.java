package com.trt.contentengagement.leaderboard.infrastructure.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.RankedXpEntry;
import com.trt.contentengagement.gamification.application.XpLeaderboardResult;
import com.trt.contentengagement.leaderboard.application.CachedLeaderboard;
import com.trt.contentengagement.leaderboard.application.LeaderboardProjectionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.leaderboard.redis-enabled", havingValue = "true")
public class RedisLeaderboardProjectionStore implements LeaderboardProjectionStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RedisLeaderboardProjectionStore.class
    );
    private static final String PREFIX = "leaderboard:v1";
    private static final String ACTIVE_GENERATION_KEY = PREFIX + ":active";

    private final StringRedisTemplate redisTemplate;

    public RedisLeaderboardProjectionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<CachedLeaderboard> findGlobal(UUID currentUserId, int limit) {
        return read("global", currentUserId, limit);
    }

    @Override
    public Optional<CachedLeaderboard> findByContent(
            UUID contentId, UUID currentUserId, int limit
    ) {
        return read("content:" + contentId, currentUserId, limit);
    }

    @Override
    public boolean replaceAll(
            List<RankedXpEntry> globalEntries,
            Map<UUID, List<RankedXpEntry>> contentEntries,
            Instant generatedAt
    ) {
        String generation = UUID.randomUUID().toString();
        Set<String> generatedKeys = new LinkedHashSet<>();
        try {
            writeScope(generation, "global", globalEntries, generatedAt, generatedKeys);
            for (Map.Entry<UUID, List<RankedXpEntry>> content : contentEntries.entrySet()) {
                writeScope(
                        generation, "content:" + content.getKey(), content.getValue(),
                        generatedAt, generatedKeys
                );
            }
            String generationKeysKey = generationKeysKey(generation);
            if (!generatedKeys.isEmpty()) {
                redisTemplate.opsForSet().add(
                        generationKeysKey, generatedKeys.toArray(String[]::new)
                );
            }
            generatedKeys.add(generationKeysKey);
            String previousGeneration = redisTemplate.opsForValue().getAndSet(
                    ACTIVE_GENERATION_KEY, generation
            );
            deleteGeneration(previousGeneration);
            return true;
        } catch (RuntimeException exception) {
            deleteKeysBestEffort(generatedKeys);
            LOGGER.warn("Could not replace leaderboard Redis projection.", exception);
            return false;
        }
    }

    private Optional<CachedLeaderboard> read(
            String scope, UUID currentUserId, int limit
    ) {
        try {
            String generation = redisTemplate.opsForValue().get(ACTIVE_GENERATION_KEY);
            if (generation == null) {
                return Optional.empty();
            }
            String scopeKey = scopeKey(generation, scope);
            String readyValue = redisTemplate.opsForValue().get(scopeKey + ":ready");
            String generatedAtValue = redisTemplate.opsForValue().get(
                    scopeKey + ":generated-at"
            );
            if (!"1".equals(readyValue) || generatedAtValue == null) {
                return Optional.empty();
            }
            String rankKey = scopeKey + ":rank";
            String entriesKey = scopeKey + ":entries";
            Set<String> leaderIds = redisTemplate.opsForZSet().range(
                    rankKey, 0, limit - 1L
            );
            if (leaderIds == null) {
                return Optional.empty();
            }
            List<RankedXpEntry> leaders = readEntries(entriesKey, leaderIds);
            Long currentUserRank = redisTemplate.opsForZSet().rank(
                    rankKey, currentUserId.toString()
            );
            RankedXpEntry currentUser = currentUserRank == null
                    ? null
                    : readEntry(entriesKey, currentUserId.toString()).orElse(null);
            Long participantCount = redisTemplate.opsForZSet().size(rankKey);
            return Optional.of(new CachedLeaderboard(
                    new XpLeaderboardResult(
                            leaders, currentUser,
                            participantCount == null ? 0 : participantCount
                    ),
                    Instant.parse(generatedAtValue)
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read leaderboard Redis projection; using PostgreSQL.");
            return Optional.empty();
        }
    }

    private void writeScope(
            String generation,
            String scope,
            List<RankedXpEntry> entries,
            Instant generatedAt,
            Set<String> generatedKeys
    ) {
        String scopeKey = scopeKey(generation, scope);
        String rankKey = scopeKey + ":rank";
        String entriesKey = scopeKey + ":entries";
        String generatedAtKey = scopeKey + ":generated-at";
        String readyKey = scopeKey + ":ready";
        generatedKeys.add(rankKey);
        generatedKeys.add(entriesKey);
        generatedKeys.add(generatedAtKey);
        generatedKeys.add(readyKey);
        for (RankedXpEntry entry : entries) {
            redisTemplate.opsForZSet().add(
                    rankKey, entry.userId().toString(), entry.position()
            );
            redisTemplate.opsForHash().put(
                    entriesKey, entry.userId().toString(), serialize(entry)
            );
        }
        redisTemplate.opsForValue().set(generatedAtKey, generatedAt.toString());
        redisTemplate.opsForValue().set(readyKey, "1");
    }

    private List<RankedXpEntry> readEntries(String entriesKey, Set<String> userIds) {
        List<String> orderedUserIds = new ArrayList<>(userIds);
        List<Object> serializedEntries = redisTemplate.opsForHash().multiGet(
                entriesKey, new ArrayList<>(orderedUserIds)
        );
        if (serializedEntries.size() != orderedUserIds.size()
                || serializedEntries.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("Leaderboard projection entry is incomplete.");
        }
        List<RankedXpEntry> entries = new ArrayList<>(orderedUserIds.size());
        for (int index = 0; index < orderedUserIds.size(); index++) {
            entries.add(deserialize(
                    orderedUserIds.get(index), serializedEntries.get(index).toString()
            ));
        }
        return entries;
    }

    private Optional<RankedXpEntry> readEntry(String entriesKey, String userId) {
        Object serialized = redisTemplate.opsForHash().get(entriesKey, userId);
        if (serialized == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(userId, serialized.toString()));
    }

    private RankedXpEntry deserialize(String userId, String serialized) {
        String[] fields = serialized.split("\\|", -1);
        if (fields.length != 4) {
            throw new IllegalStateException("Invalid leaderboard projection entry.");
        }
        try {
            return new RankedXpEntry(
                    Long.parseLong(fields[0]),
                    UUID.fromString(userId),
                    Long.parseLong(fields[1]),
                    Instant.ofEpochSecond(
                            Long.parseLong(fields[2]), Long.parseLong(fields[3])
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid leaderboard projection entry.", exception);
        }
    }

    private String serialize(RankedXpEntry entry) {
        return entry.position() + "|" + entry.totalXp() + "|"
                + entry.firstXpAt().getEpochSecond() + "|" + entry.firstXpAt().getNano();
    }

    private void deleteGeneration(String generation) {
        if (generation == null) {
            return;
        }
        try {
            String generationKeysKey = generationKeysKey(generation);
            Set<String> oldKeys = redisTemplate.opsForSet().members(generationKeysKey);
            if (oldKeys != null && !oldKeys.isEmpty()) {
                redisTemplate.delete(oldKeys);
            }
            redisTemplate.delete(generationKeysKey);
        } catch (RuntimeException cleanupFailure) {
            LOGGER.debug("Could not clean previous leaderboard Redis generation.");
        }
    }

    private void deleteKeysBestEffort(Set<String> keys) {
        try {
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException cleanupFailure) {
            LOGGER.debug("Could not clean incomplete leaderboard Redis generation.");
        }
    }

    private String scopeKey(String generation, String scope) {
        return PREFIX + ":generation:" + generation + ":" + scope;
    }

    private String generationKeysKey(String generation) {
        return PREFIX + ":generation:" + generation + ":keys";
    }
}
