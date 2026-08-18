package com.trt.contentengagement.content.application;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.identity.application.CurrentActorProvider;

@Service
public class ContentTranslationService {
    private final ContentCatalogRepository contentCatalogRepository;
    private final ContentTranslationRepository translationRepository;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;

    public ContentTranslationService(ContentCatalogRepository contentCatalogRepository,
                                     ContentTranslationRepository translationRepository,
                                     AdminAuditLog adminAuditLog,
                                     CurrentActorProvider currentActorProvider,
                                     Clock clock) {
        this.contentCatalogRepository = contentCatalogRepository;
        this.translationRepository = translationRepository;
        this.adminAuditLog = adminAuditLog;
        this.currentActorProvider = currentActorProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ContentTranslation get(UUID contentId, String languageCode) {
        requireEnglish(languageCode);
        ContentDetails source = ContentDetails.from(contentCatalogRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId)));
        return translationRepository.find(contentId, languageCode)
                .map(stored -> mergeForEditing(source, stored))
                .orElseGet(() -> blankFor(source));
    }

    @Transactional
    public ContentTranslation save(UUID contentId, String languageCode, ContentTranslation translation) {
        requireEnglish(languageCode);
        ContentDetails source = ContentDetails.from(contentCatalogRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId)));
        validateOwnership(source, translation);
        translationRepository.save(contentId, languageCode, translation);
        adminAuditLog.record(currentActorProvider.getCurrentActor().actorId(),
                "CONTENT_TRANSLATION_UPDATED", "CONTENT", contentId, clock.instant());
        return translation;
    }

    public ContentDetails localize(ContentDetails source, String languageCode) {
        if (!"en".equals(languageCode)) return source;
        return translationRepository.find(source.id(), languageCode)
                .map(translation -> apply(source, translation)).orElse(source);
    }

    public PageResult<ContentSummary> localize(PageResult<ContentSummary> page, String languageCode) {
        if (!"en".equals(languageCode)) return page;
        return new PageResult<>(page.items().stream().map(source -> translationRepository
                .find(source.id(), languageCode)
                .map(text -> new ContentSummary(source.id(), text.title(), text.description(),
                        source.contentType(), source.coverMediaId(),
                        text.coverAlternativeText() == null ? source.coverAlternativeText() : text.coverAlternativeText()))
                .orElse(source)).toList(), page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    private ContentDetails apply(ContentDetails source, ContentTranslation text) {
        Map<UUID, ContentTranslation.SeasonTranslation> seasons = text.seasons().stream()
                .collect(Collectors.toMap(ContentTranslation.SeasonTranslation::seasonId, item -> item));
        return new ContentDetails(source.id(), text.title(), text.description(), source.coverMediaId(),
                source.coverImageUrl(), text.coverAlternativeText() == null ? source.coverAlternativeText() : text.coverAlternativeText(),
                source.contentType(), source.publicationStatus(), source.createdAt(), source.updatedAt(),
                source.seasons().stream().map(season -> {
                    ContentTranslation.SeasonTranslation translated = seasons.get(season.id());
                    if (translated == null) return season;
                    Map<UUID, ContentTranslation.EpisodeTranslation> episodes = translated.episodes().stream()
                            .collect(Collectors.toMap(ContentTranslation.EpisodeTranslation::episodeId, item -> item));
                    return new ContentDetails.SeasonDetails(season.id(), season.seasonNumber(), translated.title(),
                            season.episodes().stream().map(episode -> {
                                ContentTranslation.EpisodeTranslation episodeText = episodes.get(episode.id());
                                return episodeText == null ? episode : new ContentDetails.EpisodeDetails(
                                        episode.id(), episode.episodeNumber(), episodeText.title(), episodeText.description());
                            }).toList());
                }).toList());
    }

    private void validateOwnership(ContentDetails source, ContentTranslation translation) {
        Set<UUID> seasonIds = source.seasons().stream().map(ContentDetails.SeasonDetails::id).collect(Collectors.toSet());
        Set<UUID> episodeIds = source.seasons().stream().flatMap(s -> s.episodes().stream())
                .map(ContentDetails.EpisodeDetails::id).collect(Collectors.toSet());
        boolean invalidSeason = translation.seasons().stream().anyMatch(item -> !seasonIds.contains(item.seasonId()));
        boolean invalidEpisode = translation.seasons().stream().flatMap(item -> item.episodes().stream())
                .anyMatch(item -> !episodeIds.contains(item.episodeId()));
        if (invalidSeason || invalidEpisode) throw new IllegalArgumentException("Translation contains an unrelated season or episode.");
    }

    private ContentTranslation blankFor(ContentDetails source) {
        return new ContentTranslation("", null, null, source.seasons().stream().map(season ->
                new ContentTranslation.SeasonTranslation(season.id(), "", season.episodes().stream().map(episode ->
                        new ContentTranslation.EpisodeTranslation(episode.id(), "", null)).toList())).toList());
    }

    private ContentTranslation mergeForEditing(ContentDetails source, ContentTranslation stored) {
        Map<UUID, ContentTranslation.SeasonTranslation> storedSeasons = stored.seasons().stream()
                .collect(Collectors.toMap(ContentTranslation.SeasonTranslation::seasonId, item -> item));
        return new ContentTranslation(stored.title(), stored.description(), stored.coverAlternativeText(),
                source.seasons().stream().map(season -> {
                    ContentTranslation.SeasonTranslation storedSeason = storedSeasons.get(season.id());
                    Map<UUID, ContentTranslation.EpisodeTranslation> storedEpisodes = storedSeason == null
                            ? Map.of() : storedSeason.episodes().stream().collect(Collectors.toMap(
                            ContentTranslation.EpisodeTranslation::episodeId, item -> item));
                    return new ContentTranslation.SeasonTranslation(season.id(),
                            storedSeason == null ? "" : storedSeason.title(), season.episodes().stream().map(episode ->
                            storedEpisodes.getOrDefault(episode.id(), new ContentTranslation.EpisodeTranslation(
                                    episode.id(), "", null))).toList());
                }).toList());
    }

    private void requireEnglish(String languageCode) {
        if (!"en".equals(languageCode)) throw new IllegalArgumentException("Only the English translation is editable.");
    }
}
