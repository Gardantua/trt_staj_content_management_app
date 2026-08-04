package com.trt.contentengagement.media.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_assets")
class JpaMediaAssetEntity {
    @Id private UUID id;
    @Column(name = "storage_key", nullable = false, length = 100, unique = true)
    private String storageKey;
    @Column(name = "media_type", nullable = false, length = 20) private String mediaType;
    @Column(name = "mime_type", nullable = false, length = 50) private String mimeType;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    @Column(name = "checksum_sha256", nullable = false, length = 64) private String checksumSha256;
    @Column(nullable = false) private int width;
    @Column(nullable = false) private int height;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected JpaMediaAssetEntity() { }

    JpaMediaAssetEntity(
            UUID id, String storageKey, String mediaType, String mimeType, long byteSize,
            String checksumSha256, int width, int height, UUID createdBy, Instant createdAt
    ) {
        this.id = id;
        this.storageKey = storageKey;
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.byteSize = byteSize;
        this.checksumSha256 = checksumSha256;
        this.width = width;
        this.height = height;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    UUID id() { return id; }
    String storageKey() { return storageKey; }
    String mediaType() { return mediaType; }
    String mimeType() { return mimeType; }
    long byteSize() { return byteSize; }
    String checksumSha256() { return checksumSha256; }
    int width() { return width; }
    int height() { return height; }
    UUID createdBy() { return createdBy; }
    Instant createdAt() { return createdAt; }
}
