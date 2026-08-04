package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_definitions")
class JpaQuizEntity {

    @Id
    private UUID id;
    @Column(name = "content_id", nullable = false)
    private UUID contentId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber ASC")
    private List<JpaQuizVersionEntity> versions = new ArrayList<>();

    protected JpaQuizEntity() {
    }

    JpaQuizEntity(UUID id, UUID contentId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.contentId = contentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void addVersion(JpaQuizVersionEntity version) {
        versions.add(version);
        version.attachTo(this);
    }

    UUID id() { return id; }
    UUID contentId() { return contentId; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
    List<JpaQuizVersionEntity> versions() { return versions; }
}
