package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.content.application.ContentTranslation;
import com.trt.contentengagement.content.application.ContentTranslationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcContentTranslationRepository implements ContentTranslationRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcContentTranslationRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public Optional<ContentTranslation> find(UUID contentId, String languageCode) {
        List<Map<String, Object>> roots = jdbcTemplate.queryForList("""
                SELECT title, description, cover_alternative_text
                FROM catalog_content_translations WHERE content_id = ? AND language_code = ?
                """, contentId, languageCode);
        if (roots.isEmpty()) return Optional.empty();
        Map<UUID, MutableSeason> seasons = new LinkedHashMap<>();
        jdbcTemplate.queryForList("""
                SELECT s.id season_id, st.title FROM catalog_seasons s
                JOIN catalog_season_translations st ON st.season_id = s.id AND st.language_code = ?
                WHERE s.content_id = ? ORDER BY s.season_number
                """, languageCode, contentId).forEach(row -> seasons.put((UUID) row.get("season_id"),
                new MutableSeason((UUID) row.get("season_id"), (String) row.get("title"))));
        jdbcTemplate.queryForList("""
                SELECT s.id season_id, e.id episode_id, et.title, et.description
                FROM catalog_seasons s JOIN catalog_episodes e ON e.season_id = s.id
                JOIN catalog_episode_translations et ON et.episode_id = e.id AND et.language_code = ?
                WHERE s.content_id = ? ORDER BY s.season_number, e.episode_number
                """, languageCode, contentId).forEach(row -> seasons.computeIfAbsent((UUID) row.get("season_id"),
                id -> new MutableSeason(id, "")).episodes.add(new ContentTranslation.EpisodeTranslation(
                (UUID) row.get("episode_id"), (String) row.get("title"), (String) row.get("description"))));
        Map<String, Object> root = roots.getFirst();
        return Optional.of(new ContentTranslation((String) root.get("title"), (String) root.get("description"),
                (String) root.get("cover_alternative_text"), seasons.values().stream().map(MutableSeason::freeze).toList()));
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
}
