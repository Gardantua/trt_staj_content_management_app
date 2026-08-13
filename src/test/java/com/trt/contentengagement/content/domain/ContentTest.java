package com.trt.contentengagement.content.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

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
        series.setCover(UUID.randomUUID(), "Seksenler dizisi kapak gorseli", INITIAL_TIME);

        assertThatThrownBy(() -> series.publish(INITIAL_TIME))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessageContaining("every season requires an episode");
    }

    @Test
    void publishedContentMetadataAndCoverCanBeUpdated() {
        Content film = Content.create("Kesişme", null, ContentType.FILM, INITIAL_TIME);
        film.setCover(UUID.randomUUID(), "Film kapak gorseli", INITIAL_TIME);
        film.publish(INITIAL_TIME);
        UUID replacementCoverId = UUID.randomUUID();

        film.updateDetails("Yeni Başlık", "Yeni açıklama", INITIAL_TIME.plusSeconds(1));
        film.setCover(
                replacementCoverId,
                "Yeni film kapak görseli",
                INITIAL_TIME.plusSeconds(2)
        );

        assertThat(film.title()).isEqualTo("Yeni Başlık");
        assertThat(film.description()).isEqualTo("Yeni açıklama");
        assertThat(film.coverMediaId()).isEqualTo(replacementCoverId);
    }

    @Test
    void publishedSeriesAllowsOnlyAppendingToItsHierarchy() {
        Content series = Content.create("Teşkilat", null, ContentType.SERIES, INITIAL_TIME);
        Season firstSeason = series.addSeason(1, "Birinci Sezon", INITIAL_TIME);
        Episode firstEpisode = series.addEpisode(
                firstSeason.id(), 1, "Başlangıç", null, INITIAL_TIME
        );
        series.setCover(UUID.randomUUID(), "Dizi kapak görseli", INITIAL_TIME);
        series.publish(INITIAL_TIME);

        Season secondSeason = series.addSeason(2, "İkinci Sezon", INITIAL_TIME.plusSeconds(1));
        series.addEpisode(secondSeason.id(), 1, "Yeni sezon başlangıcı", null,
                INITIAL_TIME.plusSeconds(2));
        series.addEpisode(firstSeason.id(), 2, "Devam", null, INITIAL_TIME.plusSeconds(3));

        assertThat(series.requireSeason(firstSeason.id()).episodes()).hasSize(2);
        assertThatThrownBy(() -> series.addSeason(1, "Araya sezon", INITIAL_TIME.plusSeconds(4)))
                .isInstanceOf(ContentRuleViolationException.class)
                .hasMessageContaining("after every published season");
        assertThatThrownBy(() -> series.addEpisode(
                firstSeason.id(), 1, "Araya bölüm", null, INITIAL_TIME.plusSeconds(5)
        )).isInstanceOf(ContentRuleViolationException.class)
                .hasMessageContaining("after every published episode");
        assertThatThrownBy(() -> series.updateEpisode(
                firstSeason.id(), firstEpisode.id(), 1, "Değişen", null,
                INITIAL_TIME.plusSeconds(6)
        )).isInstanceOf(ContentRuleViolationException.class)
                .hasMessage("Published content cannot be modified in place.");
    }
}
