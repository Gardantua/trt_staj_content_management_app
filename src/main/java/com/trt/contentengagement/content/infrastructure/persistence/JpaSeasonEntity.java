package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_seasons")
class JpaSeasonEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private JpaContentEntity content;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("episodeNumber ASC")
    private List<JpaEpisodeEntity> episodes = new ArrayList<>();

    protected JpaSeasonEntity() {
    }

    JpaSeasonEntity(UUID id, int seasonNumber, String title) {
        this.id = id;
        this.seasonNumber = seasonNumber;
        this.title = title;
    }

    void attachTo(JpaContentEntity content) {
        this.content = content;
    }

    void addEpisode(JpaEpisodeEntity episode) {
        episodes.add(episode);
        episode.attachTo(this);
    }

    UUID id() {
        return id;
    }

    int seasonNumber() {
        return seasonNumber;
    }

    String title() {
        return title;
    }

    List<JpaEpisodeEntity> episodes() {
        return episodes;
    }
}
