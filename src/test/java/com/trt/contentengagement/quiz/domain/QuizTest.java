package com.trt.contentengagement.quiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QuizTest {

    private static final UUID CONTENT_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
    );
    private static final Instant INITIAL_TIME = Instant.parse("2026-08-04T08:00:00Z");
    private static final Instant LATER_TIME = Instant.parse("2026-08-04T09:00:00Z");
    private static final UUID FALLBACK_MEDIA_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222222"
    );

    @Test
    void incompleteDraftCannotBePublished() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Bölüm Quizi", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();

        assertThatThrownBy(() -> quiz.publish(
                draftVersionId, FALLBACK_MEDIA_ID, "Dizi kapak gorseli", LATER_TIME
        ))
                .isInstanceOf(QuizRuleViolationException.class)
                .hasMessage("A quiz requires at least one question before publication.");
    }

    @Test
    void questionRequiresExactlyOneCorrectOptionAtPublication() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Bölüm Quizi", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();
        quiz.addQuestion(
                draftVersionId,
                1,
                "Baş karakter kimdir?",
                QuestionDifficulty.EASY,
                null, null, null, null,
                List.of(
                        new Question.OptionDraft(1, "Ali", false),
                        new Question.OptionDraft(2, "Veli", false),
                        new Question.OptionDraft(3, "Ayse", false),
                        new Question.OptionDraft(4, "Fatma", false)
                ),
                INITIAL_TIME
        );

        assertThatThrownBy(() -> quiz.publish(
                draftVersionId, FALLBACK_MEDIA_ID, "Dizi kapak gorseli", LATER_TIME
        ))
                .isInstanceOf(QuizRuleViolationException.class)
                .hasMessageContaining("exactly one correct answer option");
    }

    @Test
    void publishedVersionCannotBeModifiedInPlace() {
        Quiz quiz = publishedQuiz();
        QuizVersion publishedVersion = quiz.publishedVersion();

        assertThatThrownBy(() -> quiz.updateDraft(
                publishedVersion.id(), "Değişen başlık", null, LATER_TIME
        )).isInstanceOf(QuizRuleViolationException.class)
                .hasMessage("Published or archived quiz versions cannot be modified in place.");
    }

    @Test
    void retiringQuizArchivesPublishedVersionAndDiscardsWorkingCopy() {
        Quiz quiz = publishedQuiz();
        quiz.createDraftFromPublished(LATER_TIME);

        quiz.retire(LATER_TIME.plusSeconds(1));

        assertThat(quiz.versions()).hasSize(1);
        assertThat(quiz.versions().getFirst().status()).isEqualTo(QuizVersionStatus.ARCHIVED);
        assertThat(quiz.hasPublicationHistory()).isTrue();
    }

    @Test
    void newDraftCopiesPublishedContentWithNewStableIdentifiers() {
        Quiz quiz = publishedQuiz();
        QuizVersion publishedVersion = quiz.publishedVersion();

        QuizVersion draftVersion = quiz.createDraftFromPublished(LATER_TIME);

        assertThat(draftVersion.versionNumber()).isEqualTo(2);
        assertThat(draftVersion.status()).isEqualTo(QuizVersionStatus.DRAFT);
        assertThat(draftVersion.id()).isNotEqualTo(publishedVersion.id());
        assertThat(draftVersion.questions()).hasSize(1);
        assertThat(draftVersion.questions().getFirst().id())
                .isNotEqualTo(publishedVersion.questions().getFirst().id());
        assertThat(draftVersion.scoringPolicyVersion())
                .isEqualTo(ScoringPolicyVersion.STANDARD_V1);
    }

    @Test
    void publishingNewDraftArchivesPreviousPublishedVersion() {
        Quiz quiz = publishedQuiz();
        QuizVersion firstPublishedVersion = quiz.publishedVersion();
        QuizVersion secondDraftVersion = quiz.createDraftFromPublished(LATER_TIME);

        quiz.publish(
                secondDraftVersion.id(), FALLBACK_MEDIA_ID,
                "Dizi kapak gorseli", LATER_TIME.plusSeconds(60)
        );

        assertThat(firstPublishedVersion.status()).isEqualTo(QuizVersionStatus.ARCHIVED);
        assertThat(quiz.publishedVersion().id()).isEqualTo(secondDraftVersion.id());
    }

    @Test
    void duplicateQuestionOrderIsRejectedBeforePersistence() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Bölüm Quizi", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();
        addValidQuestion(quiz, draftVersionId);

        assertThatThrownBy(() -> addValidQuestion(quiz, draftVersionId))
                .isInstanceOf(QuizRuleViolationException.class)
                .hasMessage("Question order must be unique within a quiz version.");
    }

    @Test
    void questionRequiresExactlyFourOptions() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Dort Secenek Quizi", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();

        assertThatThrownBy(() -> quiz.addQuestion(
                draftVersionId, 1, "Eksik secenekli soru", QuestionDifficulty.MEDIUM,
                null, null, null, null,
                List.of(
                        new Question.OptionDraft(1, "A", true),
                        new Question.OptionDraft(2, "B", false),
                        new Question.OptionDraft(3, "C", false)
                ), INITIAL_TIME
        )).isInstanceOf(QuizRuleViolationException.class)
                .hasMessage("Every question must contain exactly four answer options.");
    }

    @Test
    void informativeVisualRequiresEquivalentAccessibleQuestionText() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Gorsel Quiz", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();

        assertThatThrownBy(() -> quiz.addQuestion(
                draftVersionId, 1, "Bu dagin adi nedir?", QuestionDifficulty.EASY,
                UUID.randomUUID(), VisualRole.INFORMATIVE,
                "Karla kapli bir dag", null,
                List.of(
                        new Question.OptionDraft(1, "Erciyes", true),
                        new Question.OptionDraft(2, "Uludag", false),
                        new Question.OptionDraft(3, "Agri", false),
                        new Question.OptionDraft(4, "Toros", false)
                ), INITIAL_TIME
        )).isInstanceOf(QuizRuleViolationException.class)
                .hasMessageContaining("accessible prompt");
    }

    @Test
    void accessibleVisualTextCannotRevealCorrectAnswerAtPublication() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Gorsel Quiz", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();
        quiz.addQuestion(
                draftVersionId, 1, "Bu dagin adi nedir?", QuestionDifficulty.EASY,
                UUID.randomUUID(), VisualRole.INFORMATIVE,
                "Erciyes daginin fotograifi",
                "Fotografta karli ve volkanik bir dag goruluyor. Bu dagin adi nedir?",
                List.of(
                        new Question.OptionDraft(1, "Erciyes", true),
                        new Question.OptionDraft(2, "Uludag", false),
                        new Question.OptionDraft(3, "Agri", false),
                        new Question.OptionDraft(4, "Toros", false)
                ), INITIAL_TIME
        );

        assertThatThrownBy(() -> quiz.publish(
                draftVersionId, FALLBACK_MEDIA_ID, "Dizi kapak gorseli", LATER_TIME
        )).isInstanceOf(QuizRuleViolationException.class)
                .hasMessage("Accessible visual text cannot contain the correct answer.");
    }

    @Test
    void quizScopeRequiresMatchingSeasonAndEpisodeReferences() {
        UUID seasonId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();

        Quiz episodeQuiz = Quiz.create(
                CONTENT_ID, QuizScopeType.EPISODE, seasonId, episodeId,
                "Bölüm Quizi", null, INITIAL_TIME
        );

        assertThat(episodeQuiz.scopeType()).isEqualTo(QuizScopeType.EPISODE);
        assertThat(episodeQuiz.seasonId()).isEqualTo(seasonId);
        assertThat(episodeQuiz.episodeId()).isEqualTo(episodeId);
        assertThatThrownBy(() -> Quiz.create(
                CONTENT_ID, QuizScopeType.SEASON, null, episodeId,
                "Geçersiz", null, INITIAL_TIME
        )).isInstanceOf(QuizRuleViolationException.class)
                .hasMessageContaining("scope");
    }

    private Quiz publishedQuiz() {
        Quiz quiz = Quiz.create(CONTENT_ID, "Bölüm Quizi", null, INITIAL_TIME);
        UUID draftVersionId = quiz.versions().getFirst().id();
        addValidQuestion(quiz, draftVersionId);
        quiz.publish(draftVersionId, FALLBACK_MEDIA_ID, "Dizi kapak gorseli", LATER_TIME);
        return quiz;
    }

    private void addValidQuestion(Quiz quiz, UUID versionId) {
        quiz.addQuestion(
                versionId,
                1,
                "Baş karakter kimdir?",
                QuestionDifficulty.EASY,
                null, null, null, null,
                List.of(
                        new Question.OptionDraft(1, "Ali", true),
                        new Question.OptionDraft(2, "Veli", false),
                        new Question.OptionDraft(3, "Ayse", false),
                        new Question.OptionDraft(4, "Fatma", false)
                ),
                INITIAL_TIME
        );
    }
}
