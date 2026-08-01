package com.trt.contentengagement.content.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.content.domain.ContentType;
import com.trt.contentengagement.content.domain.PublicationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_contents")
class JpaContentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private PublicationStatus publicationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seasonNumber ASC")
    private List<JpaSeasonEntity> seasons = new ArrayList<>();

    protected JpaContentEntity() {
    }

    JpaContentEntity(
            UUID id,
            String title,
            String description,
            ContentType contentType,
            PublicationStatus publicationStatus,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.publicationStatus = publicationStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void addSeason(JpaSeasonEntity season) {
        seasons.add(season);
        season.attachTo(this);
    }

    UUID id() {
        return id;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    ContentType contentType() {
        return contentType;
    }

    PublicationStatus publicationStatus() {
        return publicationStatus;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    List<JpaSeasonEntity> seasons() {
        return seasons;
    }
}
