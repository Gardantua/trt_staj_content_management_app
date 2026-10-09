package com.trt.contentengagement.content.application;

import java.util.List;
import java.util.UUID;

public record ContentTranslation(
        String title, String description, String coverAlternativeText,
        List<SeasonTranslation> seasons
) {
    public record SeasonTranslation(UUID seasonId, String title, List<EpisodeTranslation> episodes) { }
    public record EpisodeTranslation(UUID episodeId, String title, String description) { }
}
