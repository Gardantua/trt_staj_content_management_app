package com.trt.contentengagement.quiz.application;

import java.util.UUID;

import com.trt.contentengagement.quiz.domain.AnswerOption;
import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameplayQuizSnapshotService implements GameplayQuizSnapshotProvider {
    private final QuizCatalogRepository quizCatalogRepository;

    public GameplayQuizSnapshotService(QuizCatalogRepository quizCatalogRepository) {
        this.quizCatalogRepository = quizCatalogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GameplayQuizSnapshot getPublishedByQuizId(UUID quizId) {
        Quiz quiz = quizCatalogRepository.findWithPublishedVersionById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        return snapshot(quiz, quiz.publishedVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public GameplayQuizSnapshot getByVersionId(UUID versionId) {
        Quiz quiz = quizCatalogRepository.findByVersionId(versionId)
                .orElseThrow(() -> new QuizNotFoundException(versionId));
        return snapshot(quiz, quiz.requireVersion(versionId));
    }

    private GameplayQuizSnapshot snapshot(Quiz quiz, QuizVersion version) {
        return new GameplayQuizSnapshot(
                quiz.id(), version.id(), version.scoringPolicyVersion().name(),
                version.questions().stream().map(question -> new GameplayQuizSnapshot.QuestionSnapshot(
                        question.id(), question.questionOrder(), question.prompt(),
                        question.difficulty().name(),
                        visual(question, version), question.accessiblePrompt(),
                        question.answerOptions().stream().map(option ->
                                new GameplayQuizSnapshot.OptionSnapshot(
                                        option.id(), option.optionOrder(), option.text()
                                )).toList(),
                        question.answerOptions().stream().filter(AnswerOption::correct)
                                .findFirst().orElseThrow().id()
                )).toList()
        );
    }

    private GameplayQuizSnapshot.VisualSnapshot visual(
            com.trt.contentengagement.quiz.domain.Question question,
            QuizVersion version
    ) {
        UUID mediaAssetId = question.visualMediaId() == null
                ? version.fallbackMediaId() : question.visualMediaId();
        String role = question.visualMediaId() == null
                ? "DECORATIVE" : question.visualRole().name();
        String alternativeText = question.visualMediaId() == null
                ? null : question.visualAlternativeText();
        return new GameplayQuizSnapshot.VisualSnapshot(
                mediaAssetId, "/api/v1/media/" + mediaAssetId + "/content",
                role, alternativeText
        );
    }
}
