package com.trt.contentengagement.content.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.content.domain.Content;
import com.trt.contentengagement.content.domain.ContentType;
import com.trt.contentengagement.content.domain.Episode;
import com.trt.contentengagement.content.domain.PublicationStatus;
import com.trt.contentengagement.content.domain.Season;

public record ContentDetails(
        UUID id,
        String title,
        String description,
        String watchUrl,
        UUID coverMediaId,
        String coverImageUrl,
        String coverAlternativeText,
        ContentType contentType,
        PublicationStatus publicationStatus,
        Instant createdAt,
        Instant updatedAt,
        List<SeasonDetails> seasons
) {

    public static ContentDetails from(Content content) {
        return new ContentDetails(
                content.id(),
                content.title(),
                content.description(),
                content.watchUrl(),
                content.coverMediaId(),
                content.coverMediaId() == null
                        ? null : "/api/v1/media/" + content.coverMediaId() + "/content",
                content.coverAlternativeText(),
                content.contentType(),
                content.publicationStatus(),
                content.createdAt(),
                content.updatedAt(),
                content.seasons().stream().map(SeasonDetails::from).toList()
        );
    }

    public record SeasonDetails(
            UUID id,
            int seasonNumber,
            String title,
            List<EpisodeDetails> episodes
    ) {

        static SeasonDetails from(Season season) {
            return new SeasonDetails(
                    season.id(),
                    season.seasonNumber(),
                    season.title(),
                    season.episodes().stream().map(EpisodeDetails::from).toList()
            );
        }
    }

    public record EpisodeDetails(
            UUID id,
            int episodeNumber,
            String title,
            String description
    ) {

        static EpisodeDetails from(Episode episode) {
            return new EpisodeDetails(
                    episode.id(),
                    episode.episodeNumber(),
                    episode.title(),
                    episode.description()
            );
        }
    }
}
