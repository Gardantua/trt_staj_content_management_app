package com.trt.contentengagement.content.application;

import java.util.UUID;

import com.trt.contentengagement.content.domain.ContentType;

public record ContentSummary(
        UUID id, String title, String description, ContentType contentType,
        UUID coverMediaId, String coverAlternativeText
) {
}
