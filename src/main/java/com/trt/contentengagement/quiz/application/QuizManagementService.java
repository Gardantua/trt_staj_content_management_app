package com.trt.contentengagement.quiz.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.content.application.ContentReferenceVerifier;
import com.trt.contentengagement.content.application.ContentReferenceVerifier.ContentVisualReference;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.QuestionDifficulty;
import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizScopeType;
import com.trt.contentengagement.quiz.domain.QuizVersion;
import com.trt.contentengagement.quiz.domain.VisualRole;
import com.trt.contentengagement.media.application.MediaReferenceVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizManagementService {

    private final QuizCatalogRepository quizCatalogRepository;
    private final ContentReferenceVerifier contentReferenceVerifier;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final MediaReferenceVerifier mediaReferenceVerifier;
    private final Clock clock;

    public QuizManagementService(
            QuizCatalogRepository quizCatalogRepository,
            ContentReferenceVerifier contentReferenceVerifier,
            AdminAuditLog adminAuditLog,
            CurrentActorProvider currentActorProvider,
            MediaReferenceVerifier mediaReferenceVerifier,
            Clock clock
    ) {
        this.quizCatalogRepository = quizCatalogRepository;
        this.contentReferenceVerifier = contentReferenceVerifier;
        this.adminAuditLog = adminAuditLog;
        this.currentActorProvider = currentActorProvider;
        this.mediaReferenceVerifier = mediaReferenceVerifier;
        this.clock = clock;
    }

    @Transactional
    public QuizDetails create(UUID contentId, String title, String description) {
        return create(
                contentId, QuizScopeType.CONTENT, null, null, title, description
        );
    }

    @Transactional
    public QuizDetails create(
            UUID contentId,
            QuizScopeType scopeType,
            UUID seasonId,
            UUID episodeId,
            String title,
            String description
    ) {
        contentReferenceVerifier.requireQuizPlacement(contentId, seasonId, episodeId);
        Instant occurredAt = clock.instant();
        Quiz savedQuiz = quizCatalogRepository.save(Quiz.create(
                contentId, scopeType, seasonId, episodeId,
                title, description, occurredAt
        ));
        audit("QUIZ_CREATED", "QUIZ", savedQuiz.id(), occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional(readOnly = true)
    public QuizDetails get(UUID quizId) {
        return QuizDetails.from(requireQuiz(quizId));
    }

    @Transactional(readOnly = true)
    public List<AdminQuizSummary> listForContent(UUID contentId) {
        contentReferenceVerifier.requireExistingContent(contentId);
        return quizCatalogRepository.findByContentId(contentId).stream()
                .map(AdminQuizSummary::from)
                .toList();
    }

    @Transactional
    public void delete(UUID quizId) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        if (quiz.hasPublicationHistory()) {
            throw new com.trt.contentengagement.quiz.domain.QuizRuleViolationException(
                    "QUIZ_DELETE_REQUIRES_RETIREMENT",
                    "A previously published quiz must be moved to history instead of deleted."
            );
        }
        quizCatalogRepository.delete(quiz);
        audit("QUIZ_DELETED", "QUIZ", quizId, occurredAt);
    }

    @Transactional
    public QuizDetails retire(UUID quizId) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        quiz.retire(occurredAt);
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_RETIRED", "QUIZ", quizId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails createDraftVersion(UUID quizId) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        QuizVersion draftVersion = quiz.createDraftFromPublished(occurredAt);
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_VERSION_CREATED", "QUIZ_VERSION", draftVersion.id(), occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails updateDraft(
            UUID quizId,
            UUID versionId,
            String title,
            String description
    ) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        quiz.updateDraft(versionId, title, description, occurredAt);
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_VERSION_UPDATED", "QUIZ_VERSION", versionId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails addQuestion(
            UUID quizId,
            UUID versionId,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> answerOptions
    ) {
        requireVisualIfPresent(visualMediaId);
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        Question question = quiz.addQuestion(
                versionId, questionOrder, prompt, QuestionDifficulty.MEDIUM, visualMediaId, visualRole,
                visualAlternativeText, accessiblePrompt, answerOptions, occurredAt
        );
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_QUESTION_CREATED", "QUIZ_QUESTION", question.id(), occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails updateQuestion(
            UUID quizId,
            UUID versionId,
            UUID questionId,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> answerOptions
    ) {
        requireVisualIfPresent(visualMediaId);
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        quiz.updateQuestion(
                versionId, questionId, questionOrder, prompt, QuestionDifficulty.MEDIUM,
                visualMediaId, visualRole, visualAlternativeText, accessiblePrompt,
                answerOptions, occurredAt
        );
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_QUESTION_UPDATED", "QUIZ_QUESTION", questionId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails deleteQuestion(
            UUID quizId,
            UUID versionId,
            UUID questionId
    ) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        quiz.removeQuestion(versionId, questionId, occurredAt);
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_QUESTION_DELETED", "QUIZ_QUESTION", questionId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails publish(UUID quizId, UUID versionId) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        ContentVisualReference fallback = contentReferenceVerifier
                .requirePublishedVisual(quiz.contentId());
        if (fallback.mediaAssetId() == null) {
            throw new com.trt.contentengagement.quiz.domain.QuizRuleViolationException(
                    "QUIZ_CONTENT_COVER_REQUIRED",
                    "Quiz publication requires a published content cover."
            );
        }
        quiz.publish(
                versionId, fallback.mediaAssetId(), fallback.alternativeText(), occurredAt
        );
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_VERSION_PUBLISHED", "QUIZ_VERSION", versionId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    @Transactional
    public QuizDetails archive(UUID quizId, UUID versionId) {
        Instant occurredAt = clock.instant();
        Quiz quiz = requireQuiz(quizId);
        quiz.archive(versionId, occurredAt);
        Quiz savedQuiz = quizCatalogRepository.save(quiz);
        audit("QUIZ_VERSION_ARCHIVED", "QUIZ_VERSION", versionId, occurredAt);
        return QuizDetails.from(savedQuiz);
    }

    private Quiz requireQuiz(UUID quizId) {
        return quizCatalogRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
    }

    private void requireVisualIfPresent(UUID visualMediaId) {
        if (visualMediaId != null) {
            mediaReferenceVerifier.requireImage(visualMediaId);
        }
    }

    private void audit(String action, String resourceType, UUID resourceId, Instant occurredAt) {
        adminAuditLog.record(
                currentActorProvider.getCurrentActor().actorId(),
                action,
                resourceType,
                resourceId,
                occurredAt
        );
    }
}
