package com.trt.contentengagement.content.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Season {

    private final UUID id;
    private int seasonNumber;
    private String title;
    private final List<Episode> episodes;

    private Season(UUID id, int seasonNumber, String title, List<Episode> episodes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.episodes = new ArrayList<>(Objects.requireNonNull(episodes, "episodes must not be null"));
        changeDetails(seasonNumber, title);
        ensureUniqueEpisodeNumbers();
    }

    public static Season create(int seasonNumber, String title) {
        return new Season(UUID.randomUUID(), seasonNumber, title, List.of());
    }

    public static Season rehydrate(UUID id, int seasonNumber, String title, List<Episode> episodes) {
        return new Season(id, seasonNumber, title, episodes);
    }

    public void changeDetails(int seasonNumber, String title) {
        if (seasonNumber <= 0) {
            throw new ContentRuleViolationException(
                    "SEASON_NUMBER_INVALID",
                    "Season number must be greater than zero."
            );
        }
        this.seasonNumber = seasonNumber;
        this.title = ContentText.requireTitle(title);
    }

    public Episode addEpisode(int episodeNumber, String title, String description) {
        if (episodes.stream().anyMatch(episode -> episode.episodeNumber() == episodeNumber)) {
            throw new ContentRuleViolationException(
                    "EPISODE_NUMBER_DUPLICATE",
                    "Episode number must be unique within a season."
            );
        }
        Episode episode = Episode.create(episodeNumber, title, description);
        episodes.add(episode);
        return episode;
    }

    public Episode requireEpisode(UUID episodeId) {
        return episodes.stream()
                .filter(episode -> episode.id().equals(episodeId))
                .findFirst()
                .orElseThrow(() -> new ContentRuleViolationException(
                        "EPISODE_NOT_FOUND",
                        "Episode was not found."
                ));
    }

    public void updateEpisode(
            UUID episodeId,
            int episodeNumber,
            String title,
            String description
    ) {
        boolean duplicateNumber = episodes.stream()
                .anyMatch(episode -> !episode.id().equals(episodeId)
                        && episode.episodeNumber() == episodeNumber);
        if (duplicateNumber) {
            throw new ContentRuleViolationException(
                    "EPISODE_NUMBER_DUPLICATE",
                    "Episode number must be unique within a season."
            );
        }
        requireEpisode(episodeId).changeDetails(episodeNumber, title, description);
    }

    public void removeEpisode(UUID episodeId) {
        if (!episodes.removeIf(episode -> episode.id().equals(episodeId))) {
            throw new ContentRuleViolationException("EPISODE_NOT_FOUND", "Episode was not found.");
        }
    }

    public UUID id() {
        return id;
    }

    public int seasonNumber() {
        return seasonNumber;
    }

    public String title() {
        return title;
    }

    public List<Episode> episodes() {
        return episodes.stream()
                .sorted(Comparator.comparingInt(Episode::episodeNumber))
                .toList();
    }

    private void ensureUniqueEpisodeNumbers() {
        long uniqueNumbers = episodes.stream().map(Episode::episodeNumber).distinct().count();
        if (uniqueNumbers != episodes.size()) {
            throw new ContentRuleViolationException(
                    "EPISODE_NUMBER_DUPLICATE",
                    "Episode number must be unique within a season."
            );
        }
    }
}
