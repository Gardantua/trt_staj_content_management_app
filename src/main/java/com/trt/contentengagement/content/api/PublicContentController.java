package com.trt.contentengagement.content.api;

import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.content.application.ContentSummary;
import com.trt.contentengagement.content.application.PageResult;
import com.trt.contentengagement.content.application.PublicContentQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/contents")
public class PublicContentController {

    private final PublicContentQueryService publicContentQueryService;

    public PublicContentController(PublicContentQueryService publicContentQueryService) {
        this.publicContentQueryService = publicContentQueryService;
    }

    @GetMapping
    public ContentPageResponse listPublishedContents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ContentPageResponse.from(publicContentQueryService.listPublished(page, size));
    }

    @GetMapping("/{contentId}")
    public ContentResponse getPublishedContent(@PathVariable UUID contentId) {
        return ContentResponse.from(publicContentQueryService.getPublished(contentId));
    }

    public record ContentPageResponse(
            List<ContentSummaryResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {

        static ContentPageResponse from(PageResult<ContentSummary> pageResult) {
            return new ContentPageResponse(
                    pageResult.items().stream().map(ContentSummaryResponse::from).toList(),
                    pageResult.page(),
                    pageResult.size(),
                    pageResult.totalItems(),
                    pageResult.totalPages()
            );
        }
    }

    public record ContentSummaryResponse(
            String id,
            String title,
            String description,
            String contentType,
            String coverImageUrl,
            String coverAlternativeText
    ) {

        static ContentSummaryResponse from(ContentSummary contentSummary) {
            return new ContentSummaryResponse(
                    contentSummary.id().toString(),
                    contentSummary.title(),
                    contentSummary.description(),
                    contentSummary.contentType().name(),
                    "/api/v1/media/" + contentSummary.coverMediaId() + "/content",
                    contentSummary.coverAlternativeText()
            );
        }
    }
}
