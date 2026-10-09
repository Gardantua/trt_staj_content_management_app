package com.trt.contentengagement.content.application;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.content.domain.ContentType;
import com.trt.contentengagement.content.domain.PublicationStatus;

public record AdminContentSummary(
        UUID id,
        String title,
        String description,
        ContentType contentType,
        PublicationStatus publicationStatus,
        boolean hasWatchUrl,
        UUID coverMediaId,
        String coverAlternativeText,
        Instant createdAt,
        Instant updatedAt
) {
}
