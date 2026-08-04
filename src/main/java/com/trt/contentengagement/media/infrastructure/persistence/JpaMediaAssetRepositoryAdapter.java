package com.trt.contentengagement.media.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.media.application.MediaAssetRepository;
import com.trt.contentengagement.media.domain.MediaAsset;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMediaAssetRepositoryAdapter implements MediaAssetRepository {
    private final SpringDataMediaAssetRepository repository;

    public JpaMediaAssetRepositoryAdapter(SpringDataMediaAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public MediaAsset save(MediaAsset mediaAsset) {
        return toDomain(repository.save(new JpaMediaAssetEntity(
                mediaAsset.id(), mediaAsset.storageKey(), mediaAsset.mediaType(),
                mediaAsset.mimeType(), mediaAsset.byteSize(), mediaAsset.checksumSha256(),
                mediaAsset.width(), mediaAsset.height(), mediaAsset.createdBy(),
                mediaAsset.createdAt()
        )));
    }

    @Override
    public Optional<MediaAsset> findById(UUID mediaAssetId) {
        return repository.findById(mediaAssetId).map(this::toDomain);
    }

    private MediaAsset toDomain(JpaMediaAssetEntity entity) {
        return new MediaAsset(
                entity.id(), entity.storageKey(), entity.mediaType(), entity.mimeType(),
                entity.byteSize(), entity.checksumSha256(), entity.width(), entity.height(),
                entity.createdBy(), entity.createdAt()
        );
    }
}
