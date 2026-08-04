package com.trt.contentengagement.media.application;

import java.util.UUID;

public interface MediaReferenceVerifier {
    void requireImage(UUID mediaAssetId);
}
