package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.QuizCatalogRepository;
import com.trt.contentengagement.quiz.domain.AnswerOption;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;
import com.trt.contentengagement.quiz.domain.QuizVersionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class JpaQuizCatalogAdapter implements QuizCatalogRepository {

    private final SpringDataQuizRepository springDataQuizRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaQuizCatalogAdapter(SpringDataQuizRepository springDataQuizRepository) {
        this.springDataQuizRepository = springDataQuizRepository;
    }

    @Override
    public Quiz save(Quiz quiz) {
        clearPersistedCorrectAnswersBeforeAggregateMerge(quiz.id());
        return toDomain(springDataQuizRepository.save(toEntity(quiz)));
    }

    private void clearPersistedCorrectAnswersBeforeAggregateMerge(UUID quizId) {
        if (!springDataQuizRepository.existsById(quizId)) {
            return;
        }
        entityManager.createQuery("""
                update JpaAnswerOptionEntity answerOption
                set answerOption.correct = false
                where answerOption.correct = true
                  and answerOption.question.quizVersion.quiz.id = :quizId
                """)
                .setParameter("quizId", quizId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public Optional<Quiz> findById(UUID quizId) {
        return springDataQuizRepository.findById(quizId).map(this::toDomain);
    }

    @Override
    public Optional<Quiz> findWithPublishedVersionById(UUID quizId) {
        return springDataQuizRepository
                .findDistinctByIdAndVersionsStatus(quizId, QuizVersionStatus.PUBLISHED)
                .map(this::toDomain);
    }

    @Override
    public List<Quiz> findWithPublishedVersionByContentId(UUID contentId) {
        return springDataQuizRepository
                .findDistinctByContentIdAndVersionsStatusOrderByCreatedAtAsc(
                        contentId,
                        QuizVersionStatus.PUBLISHED
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Quiz> findByVersionId(UUID versionId) {
        return springDataQuizRepository.findDistinctByVersionsId(versionId).map(this::toDomain);
    }

    private JpaQuizEntity toEntity(Quiz quiz) {
        JpaQuizEntity quizEntity = new JpaQuizEntity(
                quiz.id(), quiz.contentId(), quiz.createdAt(), quiz.updatedAt()
        );
        quiz.versions().forEach(version -> {
            JpaQuizVersionEntity versionEntity = new JpaQuizVersionEntity(
                    version.id(), version.versionNumber(), version.title(),
                    version.description(), version.status(), version.scoringPolicyVersion(),
                    version.createdAt(), version.publishedAt(), version.archivedAt(),
                    version.fallbackMediaId(), version.fallbackAlternativeText()
            );
            version.questions().forEach(question -> {
                JpaQuizQuestionEntity questionEntity = new JpaQuizQuestionEntity(
                        question.id(), question.questionOrder(), question.prompt(),
                        question.difficulty(), question.visualMediaId(), question.visualRole(),
                        question.visualAlternativeText(), question.accessiblePrompt()
                );
                question.answerOptions().forEach(option -> questionEntity.addAnswerOption(
                        new JpaAnswerOptionEntity(
                                option.id(), option.optionOrder(), option.text(), option.correct()
                        )
                ));
                versionEntity.addQuestion(questionEntity);
            });
            quizEntity.addVersion(versionEntity);
        });
        return quizEntity;
    }

    private Quiz toDomain(JpaQuizEntity quizEntity) {
        return Quiz.rehydrate(
                quizEntity.id(), quizEntity.contentId(), quizEntity.createdAt(),
                quizEntity.updatedAt(),
                quizEntity.versions().stream().map(this::toDomain).toList()
        );
    }

    private QuizVersion toDomain(JpaQuizVersionEntity versionEntity) {
        return QuizVersion.rehydrate(
                versionEntity.id(), versionEntity.versionNumber(), versionEntity.title(),
                versionEntity.description(), versionEntity.status(),
                versionEntity.scoringPolicyVersion(), versionEntity.createdAt(),
                versionEntity.publishedAt(), versionEntity.archivedAt(),
                versionEntity.fallbackMediaId(), versionEntity.fallbackAlternativeText(),
                versionEntity.questions().stream().map(this::toDomain).toList()
        );
    }

    private Question toDomain(JpaQuizQuestionEntity questionEntity) {
        return new Question(
                questionEntity.id(), questionEntity.questionOrder(), questionEntity.prompt(),
                questionEntity.difficulty(), questionEntity.visualMediaId(),
                questionEntity.visualRole(), questionEntity.visualAlternativeText(),
                questionEntity.accessiblePrompt(),
                questionEntity.answerOptions().stream().map(this::toDomain).toList()
        );
    }

    private AnswerOption toDomain(JpaAnswerOptionEntity optionEntity) {
        return new AnswerOption(
                optionEntity.id(), optionEntity.optionOrder(), optionEntity.optionText(),
                optionEntity.correct()
        );
    }
}
