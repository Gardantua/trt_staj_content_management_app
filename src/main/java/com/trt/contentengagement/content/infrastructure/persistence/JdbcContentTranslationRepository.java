package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.content.application.ContentTranslation;
import com.trt.contentengagement.content.application.ContentTranslationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcContentTranslationRepository implements ContentTranslationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public JdbcContentTranslationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Optional<ContentTranslation> find(UUID contentId, String languageCode) {
        return Optional.ofNullable(findAll(Set.of(contentId), languageCode).get(contentId));
    }

    @Override
    public Map<UUID, ContentTranslation> findAll(Set<UUID> contentIds, String languageCode) {
        if (contentIds.isEmpty()) return Map.of();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("contentIds", contentIds)
                .addValue("languageCode", languageCode);
        Map<UUID, MutableContent> contents = new LinkedHashMap<>();
        namedJdbcTemplate.queryForList("""
                SELECT content_id, title, description, cover_alternative_text
                FROM catalog_content_translations
                WHERE language_code = :languageCode AND content_id IN (:contentIds)
                """, parameters).forEach(row -> contents.put((UUID) row.get("content_id"), new MutableContent(
                (String) row.get("title"), (String) row.get("description"),
                (String) row.get("cover_alternative_text"))));
        if (contents.isEmpty()) return Map.of();

        parameters.addValue("translatedContentIds", contents.keySet());
        namedJdbcTemplate.queryForList("""
                SELECT s.content_id, s.id season_id, st.title
                FROM catalog_seasons s
                JOIN catalog_season_translations st ON st.season_id = s.id AND st.language_code = :languageCode
                WHERE s.content_id IN (:translatedContentIds)
                ORDER BY s.content_id, s.season_number
                """, parameters).forEach(row -> contents.get((UUID) row.get("content_id")).seasons.put(
                (UUID) row.get("season_id"),
                new MutableSeason((UUID) row.get("season_id"), (String) row.get("title"))));
        namedJdbcTemplate.queryForList("""
                SELECT s.content_id, s.id season_id, e.id episode_id, et.title, et.description
                FROM catalog_seasons s
                JOIN catalog_episodes e ON e.season_id = s.id
                JOIN catalog_episode_translations et ON et.episode_id = e.id AND et.language_code = :languageCode
                WHERE s.content_id IN (:translatedContentIds)
                ORDER BY s.content_id, s.season_number, e.episode_number
                """, parameters).forEach(row -> {
            MutableContent content = contents.get((UUID) row.get("content_id"));
            content.seasons.computeIfAbsent((UUID) row.get("season_id"), id -> new MutableSeason(id, ""))
                    .episodes.add(new ContentTranslation.EpisodeTranslation(
                            (UUID) row.get("episode_id"), (String) row.get("title"),
                            (String) row.get("description")));
        });
        return contents.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().freeze()));
    }

    @Override
    public void save(UUID contentId, String languageCode, ContentTranslation translation) {
        jdbcTemplate.update("""
                INSERT INTO catalog_content_translations(content_id, language_code, title, description, cover_alternative_text)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (content_id, language_code) DO UPDATE SET
                title = EXCLUDED.title, description = EXCLUDED.description,
                cover_alternative_text = EXCLUDED.cover_alternative_text
                """, contentId, languageCode, translation.title(), translation.description(), translation.coverAlternativeText());
        for (ContentTranslation.SeasonTranslation season : translation.seasons()) {
            if (!season.title().isBlank()) jdbcTemplate.update("""
                    INSERT INTO catalog_season_translations(season_id, language_code, title) VALUES (?, ?, ?)
                    ON CONFLICT (season_id, language_code) DO UPDATE SET title = EXCLUDED.title
                    """, season.seasonId(), languageCode, season.title());
            for (ContentTranslation.EpisodeTranslation episode : season.episodes()) {
                if (!episode.title().isBlank()) jdbcTemplate.update("""
                        INSERT INTO catalog_episode_translations(episode_id, language_code, title, description) VALUES (?, ?, ?, ?)
                        ON CONFLICT (episode_id, language_code) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description
                        """, episode.episodeId(), languageCode, episode.title(), episode.description());
            }
        }
    }

    private static final class MutableSeason {
        private final UUID id; private final String title;
        private final List<ContentTranslation.EpisodeTranslation> episodes = new ArrayList<>();
        private MutableSeason(UUID id, String title) { this.id = id; this.title = title; }
        private ContentTranslation.SeasonTranslation freeze() { return new ContentTranslation.SeasonTranslation(id, title, List.copyOf(episodes)); }
    }

    private static final class MutableContent {
        private final String title;
        private final String description;
        private final String coverAlternativeText;
        private final Map<UUID, MutableSeason> seasons = new LinkedHashMap<>();

        private MutableContent(String title, String description, String coverAlternativeText) {
            this.title = title;
            this.description = description;
            this.coverAlternativeText = coverAlternativeText;
        }

        private ContentTranslation freeze() {
            return new ContentTranslation(title, description, coverAlternativeText,
                    seasons.values().stream().map(MutableSeason::freeze).toList());
        }
    }
}
