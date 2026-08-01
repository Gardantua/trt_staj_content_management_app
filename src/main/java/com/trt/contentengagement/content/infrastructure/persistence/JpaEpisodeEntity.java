package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_episodes")
class JpaEpisodeEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private JpaSeasonEntity season;

    @Column(name = "episode_number", nullable = false)
    private int episodeNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    protected JpaEpisodeEntity() {
    }

    JpaEpisodeEntity(UUID id, int episodeNumber, String title, String description) {
        this.id = id;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.description = description;
    }

    void attachTo(JpaSeasonEntity season) {
        this.season = season;
    }

    UUID id() {
        return id;
    }

    int episodeNumber() {
        return episodeNumber;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }
}
