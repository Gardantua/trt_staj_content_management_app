package com.trt.contentengagement.content.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ContentTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-01T12:00:00Z");

    @Test
    void seasonNumbersMustBeUniqueWithinSeries() {
        Content series = Content.create("Gönül Dağı", null, ContentType.SERIES, INITIAL_TIME);
        series.addSeason(1, "Birinci Sezon", INITIAL_TIME);

        assertThatThrownBy(() -> series.addSeason(1, "Tekrar", INITIAL_TIME))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessage("Season number must be unique within content.");
    }

    @Test
    void episodeNumbersMustBeUniqueWithinSeason() {
        Content series = Content.create("Teşkilat", null, ContentType.SERIES, INITIAL_TIME);
        Season season = series.addSeason(1, "Birinci Sezon", INITIAL_TIME);
        series.addEpisode(season.id(), 1, "Başlangıç", null, INITIAL_TIME);

        assertThatThrownBy(() -> series.addEpisode(
                season.id(),
                1,
                "Tekrar",
                null,
                INITIAL_TIME
        )).isInstanceOf(ContentRuleViolationException.class)
                .hasMessage("Episode number must be unique within a season.");
    }

    @Test
    void filmCannotContainSeasonHierarchy() {
        Content film = Content.create("Rafadan Tayfa", null, ContentType.FILM, INITIAL_TIME);

        assertThatThrownBy(() -> film.addSeason(1, "Geçersiz", INITIAL_TIME))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessage("Seasons can only be added to series content.");
    }

    @Test
    void incompleteSeriesCannotBePublished() {
        Content series = Content.create("Seksenler", null, ContentType.SERIES, INITIAL_TIME);
        series.addSeason(1, "Birinci Sezon", INITIAL_TIME);

        assertThatThrownBy(() -> series.publish(INITIAL_TIME))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessageContaining("every season requires an episode");
    }

    @Test
    void publishedContentCannotBeModifiedInPlace() {
        Content film = Content.create("Kesişme", null, ContentType.FILM, INITIAL_TIME);
        film.publish(INITIAL_TIME);

        assertThatThrownBy(() -> film.updateDetails("Yeni Başlık", null, INITIAL_TIME))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessage("Published content cannot be modified in place.");
    }
}
