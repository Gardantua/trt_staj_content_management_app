package com.trt.contentengagement.media.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MediaAsset(
        UUID id,
        String storageKey,
        String mediaType,
        String mimeType,
        long byteSize,
        String checksumSha256,
        int width,
        int height,
        UUID createdBy,
        Instant createdAt
) {
    public MediaAsset {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (storageKey == null || storageKey.isBlank() || storageKey.length() > 100) {
            throw new MediaRuleViolationException(
                    "MEDIA_INVALID_STORAGE_KEY", "Media storage key is invalid."
            );
        }
        if (!"IMAGE".equals(mediaType)) {
            throw new MediaRuleViolationException(
                    "MEDIA_TYPE_UNSUPPORTED", "Only image media is supported."
            );
        }
        if (!"image/jpeg".equals(mimeType) && !"image/png".equals(mimeType)) {
            throw new MediaRuleViolationException(
                    "MEDIA_MIME_UNSUPPORTED", "Only JPEG and PNG images are supported."
            );
        }
        if (byteSize < 1 || byteSize > 5L * 1024 * 1024) {
            throw new MediaRuleViolationException(
                    "MEDIA_SIZE_INVALID", "Image size must be between 1 byte and 5 MB."
            );
        }
        if (width < 1 || width > 4096 || height < 1 || height > 4096) {
            throw new MediaRuleViolationException(
                    "MEDIA_DIMENSIONS_INVALID", "Image dimensions cannot exceed 4096 by 4096."
            );
        }
        if (checksumSha256 == null || !checksumSha256.matches("[0-9a-f]{64}")) {
            throw new MediaRuleViolationException(
                    "MEDIA_CHECKSUM_INVALID", "Media checksum is invalid."
            );
        }
    }

    public String contentPath() {
        return "/api/v1/media/" + id + "/content";
    }
}
