package com.trt.contentengagement.leaderboardservice.infrastructure.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.application.CachedLeaderboard;
import com.trt.contentengagement.leaderboardservice.application.LeaderboardCache;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
        name = "app.leaderboard.redis-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RedisLeaderboardCache implements LeaderboardCache {
    private static final String ACTIVE_GENERATION_KEY = "leaderboard-service:v1:active";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisLeaderboardCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CachedLeaderboard> findGlobal(UUID currentUserId, int limit) {
        return find("global", currentUserId, limit);
    }

    @Override
    public Optional<CachedLeaderboard> findByContent(
            UUID contentId, UUID currentUserId, int limit
    ) {
        return find("content:" + contentId, currentUserId, limit);
    }

    @Override
    public void replace(
            List<LeaderboardEntry> global,
            Map<UUID, List<LeaderboardEntry>> contents,
            Instant generatedAt
    ) {
        String previousGeneration = redisTemplate.opsForValue().get(ACTIVE_GENERATION_KEY);
        String generation = UUID.randomUUID().toString();
        writeScope(generation, "global", global);
        for (Map.Entry<UUID, List<LeaderboardEntry>> content : contents.entrySet()) {
            writeScope(generation, "content:" + content.getKey(), content.getValue());
        }
        redisTemplate.opsForValue().set(generatedAtKey(generation), generatedAt.toString());
        redisTemplate.opsForSet().add(
                generationKeysKey(generation), generatedAtKey(generation)
        );
        redisTemplate.opsForValue().set(ACTIVE_GENERATION_KEY, generation);
        deleteGeneration(previousGeneration, generation);
    }

    private Optional<CachedLeaderboard> find(String scope, UUID currentUserId, int limit) {
        try {
            String generation = redisTemplate.opsForValue().get(ACTIVE_GENERATION_KEY);
            if (generation == null) {
                return Optional.empty();
            }
            String generatedAtText = redisTemplate.opsForValue().get(generatedAtKey(generation));
            if (generatedAtText == null) {
                return Optional.empty();
            }
            String rankingKey = rankingKey(generation, scope);
            Set<String> leadingIds = redisTemplate.opsForZSet().range(rankingKey, 0, limit - 1L);
            Long participantCount = redisTemplate.opsForZSet().zCard(rankingKey);
            if (leadingIds == null || participantCount == null) {
                return Optional.empty();
            }
            LinkedHashSet<String> requestedIds = new LinkedHashSet<>(leadingIds);
            Double currentPosition = redisTemplate.opsForZSet().score(
                    rankingKey, currentUserId.toString()
            );
            if (currentPosition != null) {
                requestedIds.add(currentUserId.toString());
            }
            List<LeaderboardEntry> entries = readEntries(generation, scope, requestedIds);
            Map<UUID, LeaderboardEntry> byUser = entries.stream().collect(
                    java.util.stream.Collectors.toMap(LeaderboardEntry::userId, entry -> entry)
            );
            List<LeaderboardEntry> leaders = leadingIds.stream()
                    .map(UUID::fromString)
                    .map(byUser::get)
                    .toList();
            if (leaders.stream().anyMatch(java.util.Objects::isNull)) {
                return Optional.empty();
            }
            return Optional.of(new CachedLeaderboard(
                    new LeaderboardResult(leaders, byUser.get(currentUserId), participantCount),
                    Instant.parse(generatedAtText)
            ));
        } catch (RuntimeException cacheFailure) {
            return Optional.empty();
        }
    }

    private void writeScope(String generation, String scope, List<LeaderboardEntry> entries) {
        String rankingKey = rankingKey(generation, scope);
        String metadataKey = metadataKey(generation, scope);
        redisTemplate.opsForSet().add(
                generationKeysKey(generation), rankingKey, metadataKey
        );
        for (LeaderboardEntry entry : entries) {
            String userId = entry.userId().toString();
            redisTemplate.opsForZSet().add(rankingKey, userId, entry.position());
            redisTemplate.opsForHash().put(
                    metadataKey, userId, objectMapper.writeValueAsString(entry)
            );
        }
    }

    private List<LeaderboardEntry> readEntries(
            String generation, String scope, Set<String> userIds
    ) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<Object> values = redisTemplate.opsForHash().multiGet(
                metadataKey(generation, scope), new ArrayList<>(userIds)
        );
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                return List.of();
            }
            entries.add(objectMapper.readValue(value.toString(), LeaderboardEntry.class));
        }
        return entries;
    }

    private String rankingKey(String generation, String scope) {
        return "leaderboard-service:v1:" + generation + ":" + scope + ":rank";
    }

    private String metadataKey(String generation, String scope) {
        return "leaderboard-service:v1:" + generation + ":" + scope + ":meta";
    }

    private String generatedAtKey(String generation) {
        return "leaderboard-service:v1:" + generation + ":generated-at";
    }

    private String generationKeysKey(String generation) {
        return "leaderboard-service:v1:" + generation + ":keys";
    }

    private void deleteGeneration(String previousGeneration, String activeGeneration) {
        if (previousGeneration == null || previousGeneration.equals(activeGeneration)) {
            return;
        }
        String keysKey = generationKeysKey(previousGeneration);
        Set<String> generationKeys = redisTemplate.opsForSet().members(keysKey);
        if (generationKeys != null && !generationKeys.isEmpty()) {
            redisTemplate.delete(generationKeys);
        }
        redisTemplate.delete(keysKey);
    }
}
