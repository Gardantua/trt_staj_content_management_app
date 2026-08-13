package com.trt.contentengagement.content.application;

import com.trt.contentengagement.content.domain.ContentRuleViolationException;

public record SeasonPlanItem(int seasonNumber, int episodeCount) {

    public SeasonPlanItem {
        if (seasonNumber < 1 || seasonNumber > 10_000) {
            throw new ContentRuleViolationException(
                    "SEASON_NUMBER_INVALID",
                    "Season number must be between 1 and 10000."
            );
        }
        if (episodeCount < 1 || episodeCount > 1_000) {
            throw new ContentRuleViolationException(
                    "EPISODE_COUNT_INVALID",
                    "Episode count must be between 1 and 1000."
            );
        }
    }
}
