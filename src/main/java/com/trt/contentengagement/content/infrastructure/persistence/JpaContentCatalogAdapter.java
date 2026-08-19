package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.content.application.AdminContentSummary;
import com.trt.contentengagement.content.application.ContentCatalogRepository;
import com.trt.contentengagement.content.application.ContentSummary;
import com.trt.contentengagement.content.application.PageResult;
import com.trt.contentengagement.content.application.WatchLinkFilter;
import com.trt.contentengagement.content.domain.Content;
import com.trt.contentengagement.content.domain.ContentRuleViolationException;
import com.trt.contentengagement.content.domain.Episode;
import com.trt.contentengagement.content.domain.PublicationStatus;
import com.trt.contentengagement.content.domain.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaContentCatalogAdapter implements ContentCatalogRepository {

    private final SpringDataContentRepository springDataContentRepository;

    public JpaContentCatalogAdapter(SpringDataContentRepository springDataContentRepository) {
        this.springDataContentRepository = springDataContentRepository;
    }

    @Override
    public Content save(Content content) {
        return toDomain(springDataContentRepository.save(toEntity(content)));
    }

    @Override
    public Optional<Content> findById(UUID contentId) {
        return springDataContentRepository.findById(contentId).map(this::toDomain);
    }

    @Override
    public Optional<Content> findPublishedById(UUID contentId) {
        return springDataContentRepository
                .findByIdAndPublicationStatus(contentId, PublicationStatus.PUBLISHED)
                .map(this::toDomain);
    }

    @Override
    public PageResult<AdminContentSummary> findAllForAdministration(
            String normalizedTitleQuery,
            WatchLinkFilter watchLinkFilter,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );
        String escapedTitleQuery = normalizedTitleQuery.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        Specification<JpaContentEntity> specification = (root, query, criteriaBuilder) ->
                normalizedTitleQuery.isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                        "%" + escapedTitleQuery + "%", '\\');
        if (watchLinkFilter == WatchLinkFilter.PRESENT) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNotNull(root.get("watchUrl")));
        } else if (watchLinkFilter == WatchLinkFilter.MISSING) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNull(root.get("watchUrl")));
        }
        Page<JpaContentEntity> contentPage = springDataContentRepository.findAll(specification, pageRequest);
        return new PageResult<>(
                contentPage.getContent().stream()
                        .map(entity -> new AdminContentSummary(
                                entity.id(),
                                entity.title(),
                                entity.description(),
                                entity.contentType(),
                                entity.publicationStatus(),
                                entity.watchUrl() != null,
                                entity.coverMediaId(),
                                entity.coverAlternativeText(),
                                entity.createdAt(),
                                entity.updatedAt()
                        ))
                        .toList(),
                contentPage.getNumber(),
                contentPage.getSize(),
                contentPage.getTotalElements(),
                contentPage.getTotalPages()
        );
    }

    @Override
    public PageResult<ContentSummary> findPublished(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<JpaContentEntity> contentPage = springDataContentRepository
                .findAllByPublicationStatus(PublicationStatus.PUBLISHED, pageRequest);
        return new PageResult<>(
                contentPage.getContent().stream()
                        .map(entity -> new ContentSummary(
                                entity.id(),
                                entity.title(),
                                entity.description(),
                                entity.contentType(),
                                entity.coverMediaId(),
                                entity.coverAlternativeText()
                        ))
                        .toList(),
                contentPage.getNumber(),
                contentPage.getSize(),
                contentPage.getTotalElements(),
                contentPage.getTotalPages()
        );
    }

    @Override
    public void delete(Content content) {
        try {
            springDataContentRepository.deleteById(content.id());
            springDataContentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ContentRuleViolationException(
                    "CONTENT_DELETE_HAS_GAMEPLAY_HISTORY",
                    "Content with gameplay history cannot be permanently deleted."
            );
        }
    }

    private JpaContentEntity toEntity(Content content) {
        JpaContentEntity contentEntity = new JpaContentEntity(
                content.id(),
                content.title(),
                content.description(),
                content.watchUrl(),
                content.coverMediaId(),
                content.coverAlternativeText(),
                content.contentType(),
                content.publicationStatus(),
                content.createdAt(),
                content.updatedAt()
        );
        content.seasons().forEach(season -> {
            JpaSeasonEntity seasonEntity = new JpaSeasonEntity(
                    season.id(),
                    season.seasonNumber(),
                    season.title()
            );
            season.episodes().forEach(episode -> seasonEntity.addEpisode(
                    new JpaEpisodeEntity(
                            episode.id(),
                            episode.episodeNumber(),
                            episode.title(),
                            episode.description()
                    )
            ));
            contentEntity.addSeason(seasonEntity);
        });
        return contentEntity;
    }

    private Content toDomain(JpaContentEntity contentEntity) {
        return Content.rehydrate(
                contentEntity.id(),
                contentEntity.title(),
                contentEntity.description(),
                contentEntity.watchUrl(),
                contentEntity.coverMediaId(),
                contentEntity.coverAlternativeText(),
                contentEntity.contentType(),
                contentEntity.publicationStatus(),
                contentEntity.createdAt(),
                contentEntity.updatedAt(),
                contentEntity.seasons().stream().map(this::toDomain).toList()
        );
    }

    private Season toDomain(JpaSeasonEntity seasonEntity) {
        return Season.rehydrate(
                seasonEntity.id(),
                seasonEntity.seasonNumber(),
                seasonEntity.title(),
                seasonEntity.episodes().stream().map(this::toDomain).toList()
        );
    }

    private Episode toDomain(JpaEpisodeEntity episodeEntity) {
        return Episode.rehydrate(
                episodeEntity.id(),
                episodeEntity.episodeNumber(),
                episodeEntity.title(),
                episodeEntity.description()
        );
    }
}
