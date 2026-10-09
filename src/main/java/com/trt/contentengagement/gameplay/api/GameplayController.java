package com.trt.contentengagement.gameplay.api;

import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.gameplay.application.AnswerSubmissionResult;
import com.trt.contentengagement.gameplay.application.AttemptDetails;
import com.trt.contentengagement.gameplay.application.GameplayService;
import com.trt.contentengagement.gameplay.application.QuizResultSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class GameplayController {
    private final GameplayService gameplayService;
    public GameplayController(GameplayService gameplayService) { this.gameplayService=gameplayService; }

    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<AttemptDetails> start(
            @PathVariable UUID quizId
    ) {
        return ResponseEntity.ok(gameplayService.start(quizId));
    }

    @GetMapping("/me/quiz-results")
    public List<QuizResultSummary> latestCompletedResults() {
        return gameplayService.latestCompletedResults();
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDetails get(@PathVariable UUID attemptId) {
        return gameplayService.get(attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public AnswerSubmissionResult answer(
            @PathVariable UUID attemptId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return gameplayService.answer(
                attemptId, request.questionId(), request.selectedOptionId(), idempotencyKey
        );
    }

    @PostMapping("/attempts/{attemptId}/timeouts")
    public AttemptDetails timeout(
            @PathVariable UUID attemptId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TimeoutQuestionRequest request
    ) {
        return gameplayService.timeout(attemptId, request.questionId(), idempotencyKey);
    }

    @PostMapping("/attempts/{attemptId}/next-question")
    public AttemptDetails startNextQuestion(@PathVariable UUID attemptId) {
        return gameplayService.startNextQuestion(attemptId);
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public AttemptDetails complete(
            @PathVariable UUID attemptId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey
    ) {
        return gameplayService.complete(attemptId);
    }

    @PostMapping("/attempts/{attemptId}/abandon")
    public AttemptDetails abandon(
            @PathVariable UUID attemptId
    ) {
        return gameplayService.abandon(attemptId);
    }

    public record SubmitAnswerRequest(
            @NotNull UUID questionId,
            @NotNull UUID selectedOptionId
    ) { }

    public record TimeoutQuestionRequest(@NotNull UUID questionId) { }
}
