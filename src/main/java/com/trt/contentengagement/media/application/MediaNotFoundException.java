package com.trt.contentengagement.media.application;

import java.util.UUID;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(UUID mediaAssetId) {
        super("Media asset was not found: " + mediaAssetId);
    }
}
