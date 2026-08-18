package com.trt.contentengagement.quiz.application;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.identity.application.CurrentActorProvider;

@Service
public class QuizTranslationService {
    private final QuizCatalogRepository quizCatalogRepository;
    private final QuizTranslationRepository translationRepository;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;

    public QuizTranslationService(QuizCatalogRepository quizCatalogRepository,
                                  QuizTranslationRepository translationRepository,
                                  AdminAuditLog adminAuditLog,
                                  CurrentActorProvider currentActorProvider,
                                  Clock clock) {
        this.quizCatalogRepository = quizCatalogRepository;
        this.translationRepository = translationRepository;
        this.adminAuditLog = adminAuditLog;
        this.currentActorProvider = currentActorProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public QuizTranslation get(UUID quizId, UUID versionId, String languageCode) {
        requireEnglish(languageCode);
        QuizVersion version = version(quizId, versionId);
        return translationRepository.find(versionId, languageCode)
                .map(stored -> mergeForEditing(version, stored)).orElseGet(() -> blankFor(version));
    }

    @Transactional
    public QuizTranslation save(UUID quizId, UUID versionId, String languageCode, QuizTranslation translation) {
        requireEnglish(languageCode);
        QuizVersion version = version(quizId, versionId);
        validateOwnership(version, translation);
        translationRepository.save(versionId, languageCode, translation);
        adminAuditLog.record(currentActorProvider.getCurrentActor().actorId(),
                "QUIZ_TRANSLATION_UPDATED", "QUIZ_VERSION", versionId, clock.instant());
        return translation;
    }

    public PublishedQuizDetails localize(PublishedQuizDetails source, String languageCode) {
        if (!"en".equals(languageCode)) return source;
        return translationRepository.find(source.versionId(), languageCode).map(text -> {
            Map<UUID, QuizTranslation.QuestionTranslation> questions = text.questions().stream()
                    .collect(Collectors.toMap(QuizTranslation.QuestionTranslation::questionId, item -> item));
            return new PublishedQuizDetails(source.quizId(), source.contentId(), source.scopeType(), source.seasonId(),
                    source.episodeId(), source.versionId(), source.versionNumber(), text.title(), text.description(),
                    source.scoringPolicyVersion(), source.questions().stream().map(question -> {
                        QuizTranslation.QuestionTranslation translated = questions.get(question.id());
                        if (translated == null) return question;
                        Map<UUID, String> options = translated.answerOptions().stream().collect(Collectors.toMap(
                                QuizTranslation.OptionTranslation::optionId, QuizTranslation.OptionTranslation::text));
                        return new PublishedQuizDetails.PublishedQuestionDetails(question.id(), question.questionOrder(),
                                translated.prompt(), question.difficulty(), new PublishedQuizDetails.PublishedVisualDetails(
                                question.visual().mediaAssetId(), question.visual().contentUrl(), question.visual().role(),
                                translated.visualAlternativeText() == null ? question.visual().alternativeText() : translated.visualAlternativeText()),
                                translated.accessiblePrompt(), question.answerOptions().stream().map(option ->
                                new PublishedQuizDetails.PublishedOptionDetails(option.id(), option.optionOrder(),
                                        options.getOrDefault(option.id(), option.text()))).toList());
                    }).toList());
        }).orElse(source);
    }

    public GameplayQuizSnapshot localize(GameplayQuizSnapshot source, String languageCode) {
        if (!"en".equals(languageCode)) return source;
        return translationRepository.find(source.versionId(), languageCode).map(text -> {
            Map<UUID, QuizTranslation.QuestionTranslation> questions = text.questions().stream()
                    .collect(Collectors.toMap(QuizTranslation.QuestionTranslation::questionId, item -> item));
            return new GameplayQuizSnapshot(source.quizId(), source.versionId(), source.scoringPolicyVersion(),
                    source.questions().stream().map(question -> {
                        QuizTranslation.QuestionTranslation translated = questions.get(question.questionId());
                        if (translated == null) return question;
                        Map<UUID, String> options = translated.answerOptions().stream().collect(Collectors.toMap(
                                QuizTranslation.OptionTranslation::optionId, QuizTranslation.OptionTranslation::text));
                        return new GameplayQuizSnapshot.QuestionSnapshot(question.questionId(), question.questionOrder(),
                                translated.prompt(), question.difficulty(), new GameplayQuizSnapshot.VisualSnapshot(
                                question.visual().mediaAssetId(), question.visual().contentUrl(), question.visual().role(),
                                translated.visualAlternativeText() == null ? question.visual().alternativeText() : translated.visualAlternativeText()),
                                translated.accessiblePrompt(), question.options().stream().map(option ->
                                new GameplayQuizSnapshot.OptionSnapshot(option.optionId(), option.optionOrder(),
                                        options.getOrDefault(option.optionId(), option.text()))).toList(), question.correctOptionId());
                    }).toList());
        }).orElse(source);
    }

    private QuizVersion version(UUID quizId, UUID versionId) {
        Quiz quiz = quizCatalogRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        return quiz.requireVersion(versionId);
    }

    private void validateOwnership(QuizVersion version, QuizTranslation translation) {
        Set<UUID> questionIds = version.questions().stream().map(question -> question.id()).collect(Collectors.toSet());
        Set<UUID> optionIds = version.questions().stream().flatMap(question -> question.answerOptions().stream())
                .map(option -> option.id()).collect(Collectors.toSet());
        if (translation.questions().stream().anyMatch(item -> !questionIds.contains(item.questionId()))
                || translation.questions().stream().flatMap(item -> item.answerOptions().stream())
                .anyMatch(item -> !optionIds.contains(item.optionId()))) {
            throw new IllegalArgumentException("Translation contains an unrelated question or answer option.");
        }
    }

    private QuizTranslation blankFor(QuizVersion version) {
        return new QuizTranslation("", null, null, version.questions().stream().map(question ->
                new QuizTranslation.QuestionTranslation(question.id(), "", null, null,
                        question.answerOptions().stream().map(option ->
                                new QuizTranslation.OptionTranslation(option.id(), "")).toList())).toList());
    }

    private QuizTranslation mergeForEditing(QuizVersion version, QuizTranslation stored) {
        Map<UUID, QuizTranslation.QuestionTranslation> storedQuestions = stored.questions().stream()
                .collect(Collectors.toMap(QuizTranslation.QuestionTranslation::questionId, item -> item));
        return new QuizTranslation(stored.title(), stored.description(), stored.fallbackAlternativeText(),
                version.questions().stream().map(question -> {
                    QuizTranslation.QuestionTranslation storedQuestion = storedQuestions.get(question.id());
                    Map<UUID, String> storedOptions = storedQuestion == null ? Map.of()
                            : storedQuestion.answerOptions().stream().collect(Collectors.toMap(
                            QuizTranslation.OptionTranslation::optionId, QuizTranslation.OptionTranslation::text));
                    return new QuizTranslation.QuestionTranslation(question.id(),
                            storedQuestion == null ? "" : storedQuestion.prompt(),
                            storedQuestion == null ? null : storedQuestion.visualAlternativeText(),
                            storedQuestion == null ? null : storedQuestion.accessiblePrompt(),
                            question.answerOptions().stream().map(option -> new QuizTranslation.OptionTranslation(
                                    option.id(), storedOptions.getOrDefault(option.id(), ""))).toList());
                }).toList());
    }

    private void requireEnglish(String languageCode) {
        if (!"en".equals(languageCode)) throw new IllegalArgumentException("Only the English translation is editable.");
    }
}
