package com.trt.contentengagement.quiz.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.QuizManagementService;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.QuestionDifficulty;
import com.trt.contentengagement.quiz.domain.VisualRole;
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
@RequestMapping("/api/v1/admin/quizzes")
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
public class AdminQuizController {

    private final QuizManagementService quizManagementService;

    public AdminQuizController(QuizManagementService quizManagementService) {
        this.quizManagementService = quizManagementService;
    }

    @PostMapping
    public ResponseEntity<AdminQuizResponse> createQuiz(
            @Valid @RequestBody CreateQuizRequest request
    ) {
        AdminQuizResponse response = AdminQuizResponse.from(quizManagementService.create(
                request.contentId(), request.title(), request.description()
        ));
        return ResponseEntity.created(URI.create("/api/v1/admin/quizzes/" + response.id()))
                .body(response);
    }

    @GetMapping("/{quizId}")
    public AdminQuizResponse getQuiz(@PathVariable UUID quizId) {
        return AdminQuizResponse.from(quizManagementService.get(quizId));
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
                request.difficulty(), request.visualMediaId(), request.visualRole(),
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
                request.difficulty(), request.visualMediaId(), request.visualRole(),
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
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }

    public record UpdateVersionRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }

    public record QuestionRequest(
            @Min(1) @Max(1000) int questionOrder,
            @NotBlank @Size(max = 1000) String prompt,
            @NotNull QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            @Size(max = 500) String visualAlternativeText,
            @Size(max = 1000) String accessiblePrompt,
            @NotNull @Size(max = 6) List<@Valid OptionRequest> answerOptions
    ) {
        List<Question.OptionDraft> toDomainOptions() {
            return answerOptions.stream()
                    .map(option -> new Question.OptionDraft(
                            option.optionOrder(), option.text(), option.correct()
                    ))
                    .toList();
        }
    }

    public record OptionRequest(
            @Min(1) @Max(6) int optionOrder,
            @NotBlank @Size(max = 500) String text,
            boolean correct
    ) {
    }
}
