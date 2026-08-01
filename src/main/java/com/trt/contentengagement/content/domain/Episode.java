package com.trt.contentengagement.content.domain;

import java.util.Objects;
import java.util.UUID;

public class Episode {

    private final UUID id;
    private int episodeNumber;
    private String title;
    private String description;

    private Episode(UUID id, int episodeNumber, String title, String description) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        changeDetails(episodeNumber, title, description);
    }

    public static Episode create(int episodeNumber, String title, String description) {
        return new Episode(UUID.randomUUID(), episodeNumber, title, description);
    }

    public static Episode rehydrate(
            UUID id,
            int episodeNumber,
            String title,
            String description
    ) {
        return new Episode(id, episodeNumber, title, description);
    }

    public void changeDetails(int episodeNumber, String title, String description) {
        if (episodeNumber <= 0) {
            throw new ContentRuleViolationException(
                    "EPISODE_NUMBER_INVALID",
                    "Episode number must be greater than zero."
            );
        }
        this.episodeNumber = episodeNumber;
        this.title = ContentText.requireTitle(title);
        this.description = ContentText.normalizeDescription(description);
    }

    public UUID id() {
        return id;
    }

    public int episodeNumber() {
        return episodeNumber;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
