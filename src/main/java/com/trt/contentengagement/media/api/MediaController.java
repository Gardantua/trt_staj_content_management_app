package com.trt.contentengagement.media.api;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import com.trt.contentengagement.media.application.MediaService;
import com.trt.contentengagement.media.domain.MediaAsset;
import com.trt.contentengagement.media.domain.MediaRuleViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/admin/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<MediaAssetResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        try {
            MediaAsset mediaAsset = mediaService.uploadImage(file.getBytes(), file.getContentType());
            return ResponseEntity.created(URI.create(mediaAsset.contentPath()))
                    .body(MediaAssetResponse.from(mediaAsset));
        } catch (IOException readFailure) {
            throw new MediaRuleViolationException(
                    "MEDIA_UPLOAD_READ_FAILED", "Uploaded image content could not be read."
            );
        }
    }

    @GetMapping("/admin/media/images")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public MediaAssetPageResponse listImages(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size
    ) {
        var result = mediaService.listImages(page, size);
        return new MediaAssetPageResponse(
                result.items().stream().map(MediaAssetResponse::from).toList(),
                result.page(), result.size(), result.totalItems(), result.totalPages()
        );
    }

    @GetMapping("/media/{mediaAssetId}/content")
    public ResponseEntity<byte[]> getContent(@PathVariable UUID mediaAssetId) {
        MediaService.MediaContent mediaContent = mediaService.getContent(mediaAssetId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaContent.mimeType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic().immutable())
                .body(mediaContent.content());
    }

    public record MediaAssetResponse(
            UUID id, String mediaType, String mimeType, long byteSize,
            int width, int height, String contentUrl, java.time.Instant createdAt
    ) {
        static MediaAssetResponse from(MediaAsset mediaAsset) {
            return new MediaAssetResponse(
                    mediaAsset.id(), mediaAsset.mediaType(), mediaAsset.mimeType(),
                    mediaAsset.byteSize(), mediaAsset.width(), mediaAsset.height(),
                    mediaAsset.contentPath(), mediaAsset.createdAt()
            );
        }
    }

    public record MediaAssetPageResponse(
            java.util.List<MediaAssetResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }
}
