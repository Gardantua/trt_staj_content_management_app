package com.trt.contentengagement.content.api;

import java.net.URI;
import java.util.UUID;

import com.trt.contentengagement.content.application.ContentManagementService;
import com.trt.contentengagement.content.application.EpisodeManagementService;
import com.trt.contentengagement.content.application.SeasonManagementService;
import com.trt.contentengagement.content.domain.ContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contents")
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
public class AdminContentController {

    private final ContentManagementService contentManagementService;
    private final SeasonManagementService seasonManagementService;
    private final EpisodeManagementService episodeManagementService;

    public AdminContentController(
            ContentManagementService contentManagementService,
            SeasonManagementService seasonManagementService,
            EpisodeManagementService episodeManagementService
    ) {
        this.contentManagementService = contentManagementService;
        this.seasonManagementService = seasonManagementService;
        this.episodeManagementService = episodeManagementService;
    }

    @PostMapping
    public ResponseEntity<ContentResponse> createContent(
            @Valid @RequestBody CreateContentRequest request
    ) {
        ContentResponse response = ContentResponse.from(contentManagementService.create(
                request.title(),
                request.description(),
                request.contentType()
        ));
        return ResponseEntity.created(URI.create("/api/v1/admin/contents/" + response.id()))
                .body(response);
    }

    @GetMapping("/{contentId}")
    public ContentResponse getContent(@PathVariable UUID contentId) {
        return ContentResponse.from(contentManagementService.get(contentId));
    }

    @PutMapping("/{contentId}")
    public ContentResponse updateContent(
            @PathVariable UUID contentId,
            @Valid @RequestBody UpdateContentRequest request
    ) {
        return ContentResponse.from(contentManagementService.update(
                contentId,
                request.title(),
                request.description()
        ));
    }

    @PostMapping("/{contentId}/publish")
    public ContentResponse publishContent(@PathVariable UUID contentId) {
        return ContentResponse.from(contentManagementService.publish(contentId));
    }

    @PutMapping("/{contentId}/cover")
    public ContentResponse setCover(
            @PathVariable UUID contentId,
            @Valid @RequestBody CoverRequest request
    ) {
        return ContentResponse.from(contentManagementService.setCover(
                contentId, request.mediaAssetId(), request.alternativeText()
        ));
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
        contentManagementService.delete(contentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{contentId}/seasons")
    public ResponseEntity<ContentResponse> addSeason(
            @PathVariable UUID contentId,
            @Valid @RequestBody SeasonRequest request
    ) {
        ContentResponse response = ContentResponse.from(seasonManagementService.add(
                contentId,
                request.seasonNumber(),
                request.title()
        ));
        String seasonId = response.seasons().stream()
                .filter(season -> season.seasonNumber() == request.seasonNumber())
                .findFirst()
                .orElseThrow()
                .id();
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId
        )).body(response);
    }

    @PutMapping("/{contentId}/seasons/{seasonId}")
    public ContentResponse updateSeason(
            @PathVariable UUID contentId,
            @PathVariable UUID seasonId,
            @Valid @RequestBody SeasonRequest request
    ) {
        return ContentResponse.from(seasonManagementService.update(
                contentId,
                seasonId,
                request.seasonNumber(),
                request.title()
        ));
    }

    @DeleteMapping("/{contentId}/seasons/{seasonId}")
    public ResponseEntity<Void> deleteSeason(
            @PathVariable UUID contentId,
            @PathVariable UUID seasonId
    ) {
        seasonManagementService.delete(contentId, seasonId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{contentId}/seasons/{seasonId}/episodes")
    public ResponseEntity<ContentResponse> addEpisode(
            @PathVariable UUID contentId,
            @PathVariable UUID seasonId,
            @Valid @RequestBody EpisodeRequest request
    ) {
        ContentResponse response = ContentResponse.from(episodeManagementService.add(
                contentId,
                seasonId,
                request.episodeNumber(),
                request.title(),
                request.description()
        ));
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/contents/" + contentId + "/seasons/" + seasonId + "/episodes"
        )).body(response);
    }

    @PutMapping("/{contentId}/seasons/{seasonId}/episodes/{episodeId}")
    public ContentResponse updateEpisode(
            @PathVariable UUID contentId,
            @PathVariable UUID seasonId,
            @PathVariable UUID episodeId,
            @Valid @RequestBody EpisodeRequest request
    ) {
        return ContentResponse.from(episodeManagementService.update(
                contentId,
                seasonId,
                episodeId,
                request.episodeNumber(),
                request.title(),
                request.description()
        ));
    }

    @DeleteMapping("/{contentId}/seasons/{seasonId}/episodes/{episodeId}")
    public ResponseEntity<Void> deleteEpisode(
            @PathVariable UUID contentId,
            @PathVariable UUID seasonId,
            @PathVariable UUID episodeId
    ) {
        episodeManagementService.delete(contentId, seasonId, episodeId);
        return ResponseEntity.noContent().build();
    }

    public record CreateContentRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull ContentType contentType
    ) {
    }

    public record UpdateContentRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }

    public record CoverRequest(
            @NotNull UUID mediaAssetId,
            @NotBlank @Size(max = 500) String alternativeText
    ) {
    }

    public record SeasonRequest(
            @Min(1) @Max(10000) int seasonNumber,
            @NotBlank @Size(max = 200) String title
    ) {
    }

    public record EpisodeRequest(
            @Min(1) @Max(100000) int episodeNumber,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }
}
