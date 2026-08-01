package com.trt.contentengagement.content.api;

import java.time.Instant;
import java.util.List;

import com.trt.contentengagement.content.application.ContentDetails;

public record ContentResponse(
        String id,
        String title,
        String description,
        String contentType,
        String publicationStatus,
        Instant createdAt,
        Instant updatedAt,
        List<SeasonResponse> seasons
) {

    public static ContentResponse from(ContentDetails contentDetails) {
        return new ContentResponse(
                contentDetails.id().toString(),
                contentDetails.title(),
                contentDetails.description(),
                contentDetails.contentType().name(),
                contentDetails.publicationStatus().name(),
                contentDetails.createdAt(),
                contentDetails.updatedAt(),
                contentDetails.seasons().stream().map(SeasonResponse::from).toList()
        );
    }

    public record SeasonResponse(
            String id,
            int seasonNumber,
            String title,
            List<EpisodeResponse> episodes
    ) {

        static SeasonResponse from(ContentDetails.SeasonDetails seasonDetails) {
            return new SeasonResponse(
                    seasonDetails.id().toString(),
                    seasonDetails.seasonNumber(),
                    seasonDetails.title(),
                    seasonDetails.episodes().stream().map(EpisodeResponse::from).toList()
            );
        }
    }

    public record EpisodeResponse(
            String id,
            int episodeNumber,
            String title,
            String description
    ) {

        static EpisodeResponse from(ContentDetails.EpisodeDetails episodeDetails) {
            return new EpisodeResponse(
                    episodeDetails.id().toString(),
                    episodeDetails.episodeNumber(),
                    episodeDetails.title(),
                    episodeDetails.description()
            );
        }
    }
}
