package com.trt.contentengagement.content.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Content {

    private final UUID id;
    private String title;
    private String description;
    private UUID coverMediaId;
    private String coverAlternativeText;
    private final ContentType contentType;
    private PublicationStatus publicationStatus;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Season> seasons;

    private Content(
            UUID id,
            String title,
            String description,
            UUID coverMediaId,
            String coverAlternativeText,
            ContentType contentType,
            PublicationStatus publicationStatus,
            Instant createdAt,
            Instant updatedAt,
            List<Season> seasons
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        this.publicationStatus = Objects.requireNonNull(
                publicationStatus,
                "publicationStatus must not be null"
        );
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.seasons = new ArrayList<>(Objects.requireNonNull(seasons, "seasons must not be null"));
        this.title = ContentText.requireTitle(title);
        this.description = ContentText.normalizeDescription(description);
        this.coverMediaId = coverMediaId;
        this.coverAlternativeText = normalizeCoverAlternativeText(
                coverMediaId, coverAlternativeText
        );
        ensureUniqueSeasonNumbers();
    }

    public static Content create(
            String title,
            String description,
            ContentType contentType,
            Instant createdAt
    ) {
        return new Content(
                UUID.randomUUID(),
                title,
                description,
                null,
                null,
                contentType,
                PublicationStatus.DRAFT,
                createdAt,
                createdAt,
                List.of()
        );
    }

    public static Content rehydrate(
            UUID id,
            String title,
            String description,
            UUID coverMediaId,
            String coverAlternativeText,
            ContentType contentType,
            PublicationStatus publicationStatus,
            Instant createdAt,
            Instant updatedAt,
            List<Season> seasons
    ) {
        return new Content(
                id,
                title,
                description,
                coverMediaId,
                coverAlternativeText,
                contentType,
                publicationStatus,
                createdAt,
                updatedAt,
                seasons
        );
    }

    public void updateDetails(String title, String description, Instant changedAt) {
        ensureDraft();
        this.title = ContentText.requireTitle(title);
        this.description = ContentText.normalizeDescription(description);
        this.updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public void setCover(UUID mediaAssetId, String alternativeText, Instant changedAt) {
        ensureDraft();
        this.coverMediaId = Objects.requireNonNull(mediaAssetId, "mediaAssetId must not be null");
        this.coverAlternativeText = normalizeCoverAlternativeText(mediaAssetId, alternativeText);
        this.updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public Season addSeason(int seasonNumber, String title, Instant changedAt) {
        ensureSeriesDraft();
        if (seasons.stream().anyMatch(season -> season.seasonNumber() == seasonNumber)) {
            throw new ContentRuleViolationException(
                    "SEASON_NUMBER_DUPLICATE",
                    "Season number must be unique within content."
            );
        }
        Season season = Season.create(seasonNumber, title);
        seasons.add(season);
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        return season;
    }

    public Season requireSeason(UUID seasonId) {
        return seasons.stream()
                .filter(season -> season.id().equals(seasonId))
                .findFirst()
                .orElseThrow(() -> new ContentRuleViolationException(
                        "SEASON_NOT_FOUND",
                        "Season was not found."
                ));
    }

    public void updateSeason(UUID seasonId, int seasonNumber, String title, Instant changedAt) {
        ensureSeriesDraft();
        boolean duplicateNumber = seasons.stream()
                .anyMatch(season -> !season.id().equals(seasonId)
                        && season.seasonNumber() == seasonNumber);
        if (duplicateNumber) {
            throw new ContentRuleViolationException(
                    "SEASON_NUMBER_DUPLICATE",
                    "Season number must be unique within content."
            );
        }
        requireSeason(seasonId).changeDetails(seasonNumber, title);
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public void removeSeason(UUID seasonId, Instant changedAt) {
        ensureSeriesDraft();
        if (!seasons.removeIf(season -> season.id().equals(seasonId))) {
            throw new ContentRuleViolationException("SEASON_NOT_FOUND", "Season was not found.");
        }
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public Episode addEpisode(
            UUID seasonId,
            int episodeNumber,
            String title,
            String description,
            Instant changedAt
    ) {
        ensureSeriesDraft();
        Episode episode = requireSeason(seasonId).addEpisode(episodeNumber, title, description);
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        return episode;
    }

    public void updateEpisode(
            UUID seasonId,
            UUID episodeId,
            int episodeNumber,
            String title,
            String description,
            Instant changedAt
    ) {
        ensureSeriesDraft();
        requireSeason(seasonId).updateEpisode(episodeId, episodeNumber, title, description);
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public void removeEpisode(UUID seasonId, UUID episodeId, Instant changedAt) {
        ensureSeriesDraft();
        requireSeason(seasonId).removeEpisode(episodeId);
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public void publish(Instant changedAt) {
        ensureDraft();
        if (coverMediaId == null) {
            throw new ContentRuleViolationException(
                    "CONTENT_COVER_REQUIRED",
                    "Published content requires an accessible cover image."
            );
        }
        if (contentType == ContentType.SERIES
                && (seasons.isEmpty() || seasons.stream().anyMatch(season -> season.episodes().isEmpty()))) {
            throw new ContentRuleViolationException(
                    "CONTENT_INCOMPLETE",
                    "A series requires at least one season and every season requires an episode."
            );
        }
        publicationStatus = PublicationStatus.PUBLISHED;
        updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public UUID coverMediaId() { return coverMediaId; }

    public String coverAlternativeText() { return coverAlternativeText; }

    public ContentType contentType() {
        return contentType;
    }

    public PublicationStatus publicationStatus() {
        return publicationStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<Season> seasons() {
        return seasons.stream()
                .sorted(Comparator.comparingInt(Season::seasonNumber))
                .toList();
    }

    private void ensureDraft() {
        if (publicationStatus != PublicationStatus.DRAFT) {
            throw new ContentRuleViolationException(
                    "PUBLISHED_CONTENT_IMMUTABLE",
                    "Published content cannot be modified in place."
            );
        }
    }

    private void ensureSeriesDraft() {
        ensureDraft();
        if (contentType != ContentType.SERIES) {
            throw new ContentRuleViolationException(
                    "SEASON_NOT_ALLOWED",
                    "Seasons can only be added to series content."
            );
        }
    }

    private void ensureUniqueSeasonNumbers() {
        long uniqueNumbers = seasons.stream().map(Season::seasonNumber).distinct().count();
        if (uniqueNumbers != seasons.size()) {
            throw new ContentRuleViolationException(
                    "SEASON_NUMBER_DUPLICATE",
                    "Season number must be unique within content."
            );
        }
    }

    private String normalizeCoverAlternativeText(UUID mediaAssetId, String alternativeText) {
        if (mediaAssetId == null) {
            if (alternativeText != null) {
                throw new ContentRuleViolationException(
                        "CONTENT_COVER_INVALID", "Cover text cannot exist without a cover image."
                );
            }
            return null;
        }
        if (alternativeText == null || alternativeText.isBlank()) {
            throw new ContentRuleViolationException(
                    "CONTENT_COVER_ALT_REQUIRED", "Cover image requires alternative text."
            );
        }
        String normalized = alternativeText.trim();
        if (normalized.length() > 500) {
            throw new ContentRuleViolationException(
                    "CONTENT_COVER_ALT_TOO_LONG", "Cover alternative text cannot exceed 500 characters."
            );
        }
        return normalized;
    }
}
