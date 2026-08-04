package com.trt.contentengagement.content.application;

import java.util.UUID;

public interface ContentReferenceVerifier {

    void requireExistingContent(UUID contentId);

    ContentVisualReference requirePublishedVisual(UUID contentId);

    record ContentVisualReference(UUID mediaAssetId, String alternativeText) { }
}
