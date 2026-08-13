package com.trt.contentengagement.media.application;

import java.util.List;

import com.trt.contentengagement.media.domain.MediaAsset;

public record MediaAssetPage(
        List<MediaAsset> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
