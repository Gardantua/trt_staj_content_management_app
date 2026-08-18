package com.trt.contentengagement.quiz.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.QuizManagementService;
import com.trt.contentengagement.quiz.application.QuizTranslation;
import com.trt.contentengagement.quiz.application.QuizTranslationService;
import com.trt.contentengagement.quiz.application.AdminQuizSummary;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.QuestionDifficulty;
import com.trt.contentengagement.quiz.domain.VisualRole;
import com.trt.contentengagement.quiz.domain.QuizScopeType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/quizzes")
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
public class AdminQuizController {

    private final QuizManagementService quizManagementService;
    private final QuizTranslationService quizTranslationService;

    public AdminQuizController(QuizManagementService quizManagementService,
                               QuizTranslationService quizTranslationService) {
        this.quizManagementService = quizManagementService;
        this.quizTranslationService = quizTranslationService;
    }

    @GetMapping("/{quizId}/versions/{versionId}/translations/{languageCode}")
    public QuizTranslation getTranslation(@PathVariable UUID quizId, @PathVariable UUID versionId,
                                          @PathVariable String languageCode) {
        return quizTranslationService.get(quizId, versionId, languageCode);
    }

    @PutMapping("/{quizId}/versions/{versionId}/translations/{languageCode}")
    public QuizTranslation saveTranslation(@PathVariable UUID quizId, @PathVariable UUID versionId,
                                           @PathVariable String languageCode,
                                           @Valid @RequestBody QuizTranslationRequest request) {
        return quizTranslationService.save(quizId, versionId, languageCode, request.toTranslation());
    }

    @PostMapping
    public ResponseEntity<AdminQuizResponse> createQuiz(
            @Valid @RequestBody CreateQuizRequest request
    ) {
        AdminQuizResponse response = AdminQuizResponse.from(quizManagementService.create(
                request.contentId(), request.resolvedScopeType(), request.seasonId(),
                request.episodeId(), request.title(), request.description()
        ));
        return ResponseEntity.created(URI.create("/api/v1/admin/quizzes/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<AdminQuizSummaryResponse> listQuizzes(@RequestParam UUID contentId) {
        return quizManagementService.listForContent(contentId).stream()
                .map(AdminQuizSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{quizId}")
    public AdminQuizResponse getQuiz(@PathVariable UUID quizId) {
        return AdminQuizResponse.from(quizManagementService.get(quizId));
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable UUID quizId) {
        quizManagementService.delete(quizId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{quizId}/retire")
    public AdminQuizResponse retireQuiz(@PathVariable UUID quizId) {
        return AdminQuizResponse.from(quizManagementService.retire(quizId));
    }

    @PostMapping("/{quizId}/versions")
    public AdminQuizResponse createDraftVersion(@PathVariable UUID quizId) {
        return AdminQuizResponse.from(quizManagementService.createDraftVersion(quizId));
    }

    @PutMapping("/{quizId}/versions/{versionId}")
    public AdminQuizResponse updateDraft(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateVersionRequest request
    ) {
        return AdminQuizResponse.from(quizManagementService.updateDraft(
                quizId, versionId, request.title(), request.description()
        ));
    }

    @PostMapping("/{quizId}/versions/{versionId}/questions")
    public AdminQuizResponse addQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId,
            @Valid @RequestBody QuestionRequest request
    ) {
        return AdminQuizResponse.from(quizManagementService.addQuestion(
                quizId, versionId, request.questionOrder(), request.prompt(),
                request.resolvedDifficulty(), request.visualMediaId(), request.visualRole(),
                request.visualAlternativeText(), request.accessiblePrompt(),
                request.toDomainOptions()
        ));
    }

    @PutMapping("/{quizId}/versions/{versionId}/questions/{questionId}")
    public AdminQuizResponse updateQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionRequest request
    ) {
        return AdminQuizResponse.from(quizManagementService.updateQuestion(
                quizId, versionId, questionId, request.questionOrder(), request.prompt(),
                request.resolvedDifficulty(), request.visualMediaId(), request.visualRole(),
                request.visualAlternativeText(), request.accessiblePrompt(),
                request.toDomainOptions()
        ));
    }

    @DeleteMapping("/{quizId}/versions/{versionId}/questions/{questionId}")
    public AdminQuizResponse deleteQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId,
            @PathVariable UUID questionId
    ) {
        return AdminQuizResponse.from(quizManagementService.deleteQuestion(
                quizId, versionId, questionId
        ));
    }

    @PostMapping("/{quizId}/versions/{versionId}/publish")
    public AdminQuizResponse publishVersion(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId
    ) {
        return AdminQuizResponse.from(quizManagementService.publish(quizId, versionId));
    }

    @PostMapping("/{quizId}/versions/{versionId}/archive")
    public AdminQuizResponse archiveVersion(
            @PathVariable UUID quizId,
            @PathVariable UUID versionId
    ) {
        return AdminQuizResponse.from(quizManagementService.archive(quizId, versionId));
    }

    public record CreateQuizRequest(
            @NotNull UUID contentId,
            QuizScopeType scopeType,
            UUID seasonId,
            UUID episodeId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
        QuizScopeType resolvedScopeType() {
            return scopeType == null ? QuizScopeType.CONTENT : scopeType;
        }
    }

    public record AdminQuizSummaryResponse(
            String id,
            String contentId,
            String scopeType,
            String seasonId,
            String episodeId,
            String title,
            String status,
            int versionNumber,
            int questionCount,
            java.time.Instant updatedAt
    ) {
        static AdminQuizSummaryResponse from(AdminQuizSummary summary) {
            return new AdminQuizSummaryResponse(
                    summary.id().toString(), summary.contentId().toString(),
                    summary.scopeType(),
                    summary.seasonId() == null ? null : summary.seasonId().toString(),
                    summary.episodeId() == null ? null : summary.episodeId().toString(),
                    summary.title(), summary.status(), summary.versionNumber(),
                    summary.questionCount(), summary.updatedAt()
            );
        }
    }

    public record UpdateVersionRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }

    public record QuizTranslationRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Size(max = 500) String fallbackAlternativeText,
            @NotNull List<@Valid QuestionTranslationRequest> questions
    ) {
        QuizTranslation toTranslation() { return new QuizTranslation(title.trim(), description,
                fallbackAlternativeText, questions.stream().map(QuestionTranslationRequest::toTranslation).toList()); }
    }
    public record QuestionTranslationRequest(
            @NotNull UUID questionId, @NotBlank @Size(max = 1000) String prompt,
            @Size(max = 500) String visualAlternativeText, @Size(max = 1000) String accessiblePrompt,
            @NotNull @Size(min = 4, max = 4) List<@Valid OptionTranslationRequest> answerOptions
    ) {
        QuizTranslation.QuestionTranslation toTranslation() { return new QuizTranslation.QuestionTranslation(
                questionId, prompt.trim(), visualAlternativeText, accessiblePrompt,
                answerOptions.stream().map(OptionTranslationRequest::toTranslation).toList()); }
    }
    public record OptionTranslationRequest(@NotNull UUID optionId, @NotBlank @Size(max = 500) String text) {
        QuizTranslation.OptionTranslation toTranslation() { return new QuizTranslation.OptionTranslation(optionId, text.trim()); }
    }

    public record QuestionRequest(
            @Min(1) @Max(1000) int questionOrder,
            @NotBlank @Size(max = 1000) String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            @Size(max = 500) String visualAlternativeText,
            @Size(max = 1000) String accessiblePrompt,
            @NotNull @Size(min = 4, max = 4) List<@Valid OptionRequest> answerOptions
    ) {
        QuestionDifficulty resolvedDifficulty() {
            return QuestionDifficulty.MEDIUM;
        }

        List<Question.OptionDraft> toDomainOptions() {
            return answerOptions.stream()
                    .map(option -> new Question.OptionDraft(
                            option.optionOrder(), option.text(), option.correct()
                    ))
                    .toList();
        }
    }

    public record OptionRequest(
            @Min(1) @Max(4) int optionOrder,
            @NotBlank @Size(max = 500) String text,
            boolean correct
    ) {
    }
}
