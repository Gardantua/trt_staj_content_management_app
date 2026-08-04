package com.trt.contentengagement.media.application;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.media.domain.MediaAsset;

public interface MediaAssetRepository {
    MediaAsset save(MediaAsset mediaAsset);
    Optional<MediaAsset> findById(UUID mediaAssetId);
}
