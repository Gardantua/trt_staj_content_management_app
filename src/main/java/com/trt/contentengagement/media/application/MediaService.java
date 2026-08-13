package com.trt.contentengagement.media.application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.media.domain.MediaAsset;
import com.trt.contentengagement.media.domain.MediaRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaService implements MediaReferenceVerifier {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaBinaryStorage mediaBinaryStorage;
    private final CurrentActorProvider currentActorProvider;
    private final AdminAuditLog adminAuditLog;
    private final Clock clock;

    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            MediaBinaryStorage mediaBinaryStorage,
            CurrentActorProvider currentActorProvider,
            AdminAuditLog adminAuditLog,
            Clock clock
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaBinaryStorage = mediaBinaryStorage;
        this.currentActorProvider = currentActorProvider;
        this.adminAuditLog = adminAuditLog;
        this.clock = clock;
    }

    @Transactional
    public MediaAsset uploadImage(byte[] content, String claimedMimeType) {
        validateByteSize(content);
        String detectedMimeType = detectMimeType(content);
        if (!detectedMimeType.equals(claimedMimeType)) {
            throw new MediaRuleViolationException(
                    "MEDIA_MIME_MISMATCH", "Declared image type does not match file content."
            );
        }
        BufferedImage image = decodeImage(content);
        UUID mediaAssetId = UUID.randomUUID();
        String storageKey = mediaAssetId.toString();
        UUID actorId = currentActorProvider.getCurrentActor().actorId();
        MediaAsset mediaAsset = new MediaAsset(
                mediaAssetId, storageKey, "IMAGE", detectedMimeType, content.length,
                sha256(content), image.getWidth(), image.getHeight(), actorId, clock.instant()
        );
        mediaBinaryStorage.store(storageKey, content);
        MediaAsset savedMediaAsset = mediaAssetRepository.save(mediaAsset);
        adminAuditLog.record(
                actorId, "MEDIA_IMAGE_UPLOADED", "MEDIA_ASSET", savedMediaAsset.id(),
                savedMediaAsset.createdAt()
        );
        return savedMediaAsset;
    }

    @Transactional(readOnly = true)
    public MediaContent getContent(UUID mediaAssetId) {
        MediaAsset mediaAsset = requireAsset(mediaAssetId);
        return new MediaContent(mediaAsset.mimeType(), mediaBinaryStorage.read(mediaAsset.storageKey()));
    }

    @Transactional(readOnly = true)
    public MediaAssetPage listImages(int page, int size) {
        return mediaAssetRepository.findPage(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireImage(UUID mediaAssetId) {
        MediaAsset mediaAsset = requireAsset(mediaAssetId);
        if (!"IMAGE".equals(mediaAsset.mediaType())) {
            throw new MediaRuleViolationException(
                    "MEDIA_TYPE_UNSUPPORTED", "The referenced media is not an image."
            );
        }
    }

    private MediaAsset requireAsset(UUID mediaAssetId) {
        return mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new MediaNotFoundException(mediaAssetId));
    }

    private void validateByteSize(byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_BYTES) {
            throw new MediaRuleViolationException(
                    "MEDIA_SIZE_INVALID", "Image size must be between 1 byte and 5 MB."
            );
        }
    }

    private String detectMimeType(byte[] content) {
        if (content.length >= 8
                && (content[0] & 0xff) == 0x89 && content[1] == 0x50
                && content[2] == 0x4e && content[3] == 0x47
                && content[4] == 0x0d && content[5] == 0x0a
                && content[6] == 0x1a && content[7] == 0x0a) {
            return "image/png";
        }
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        throw new MediaRuleViolationException(
                "MEDIA_MIME_UNSUPPORTED", "Only valid JPEG and PNG images are supported."
        );
    }

    private BufferedImage decodeImage(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new MediaRuleViolationException(
                        "MEDIA_IMAGE_INVALID", "The uploaded file is not a decodable image."
                );
            }
            if (image.getWidth() > 4096 || image.getHeight() > 4096) {
                throw new MediaRuleViolationException(
                        "MEDIA_DIMENSIONS_INVALID", "Image dimensions cannot exceed 4096 by 4096."
                );
            }
            return image;
        } catch (IOException imageReadFailure) {
            throw new MediaRuleViolationException(
                    "MEDIA_IMAGE_INVALID", "The uploaded image could not be decoded."
            );
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException unavailableAlgorithm) {
            throw new IllegalStateException("SHA-256 must be available.", unavailableAlgorithm);
        }
    }

    public record MediaContent(String mimeType, byte[] content) {
        public MediaContent {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
