package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.QuizTranslation;
import com.trt.contentengagement.quiz.application.QuizTranslationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuizTranslationRepository implements QuizTranslationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    public JdbcQuizTranslationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Optional<QuizTranslation> find(UUID versionId, String languageCode) {
        return Optional.ofNullable(findAll(Set.of(versionId), languageCode).get(versionId));
    }

    @Override
    public Map<UUID, QuizTranslation> findAll(Set<UUID> versionIds, String languageCode) {
        if (versionIds.isEmpty()) return Map.of();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("versionIds", versionIds)
                .addValue("languageCode", languageCode);
        Map<UUID, MutableQuizTranslation> translations = new LinkedHashMap<>();
        namedJdbcTemplate.queryForList("""
                SELECT quiz_version_id, title, description, fallback_alternative_text
                FROM quiz_version_translations
                WHERE language_code = :languageCode AND quiz_version_id IN (:versionIds)
                """, parameters).forEach(row -> translations.put((UUID) row.get("quiz_version_id"),
                new MutableQuizTranslation((String) row.get("title"), (String) row.get("description"),
                        (String) row.get("fallback_alternative_text"))));
        if (translations.isEmpty()) return Map.of();

        parameters.addValue("translatedVersionIds", translations.keySet());
        namedJdbcTemplate.queryForList("""
                SELECT q.quiz_version_id, q.id question_id, qt.prompt,
                       qt.visual_alternative_text, qt.accessible_prompt
                FROM quiz_questions q
                JOIN quiz_question_translations qt ON qt.question_id = q.id
                  AND qt.language_code = :languageCode
                WHERE q.quiz_version_id IN (:translatedVersionIds)
                ORDER BY q.quiz_version_id, q.question_order
                """, parameters).forEach(row -> translations.get((UUID) row.get("quiz_version_id")).questions.put(
                (UUID) row.get("question_id"), new MutableQuestion((UUID) row.get("question_id"),
                        (String) row.get("prompt"), (String) row.get("visual_alternative_text"),
                        (String) row.get("accessible_prompt"))));
        namedJdbcTemplate.queryForList("""
                SELECT q.quiz_version_id, q.id question_id, o.id option_id, ot.option_text
                FROM quiz_questions q
                JOIN quiz_answer_options o ON o.question_id = q.id
                JOIN quiz_answer_option_translations ot ON ot.answer_option_id = o.id
                  AND ot.language_code = :languageCode
                WHERE q.quiz_version_id IN (:translatedVersionIds)
                ORDER BY q.quiz_version_id, q.question_order, o.option_order
                """, parameters).forEach(row -> translations.get((UUID) row.get("quiz_version_id")).questions
                .computeIfAbsent((UUID) row.get("question_id"), id -> new MutableQuestion(id, "", null, null))
                .options.add(new QuizTranslation.OptionTranslation(
                        (UUID) row.get("option_id"), (String) row.get("option_text"))));
        return translations.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().freeze()));
    }

    @Override
    public void save(UUID versionId, String languageCode, QuizTranslation translation) {
        jdbcTemplate.update("""
                INSERT INTO quiz_version_translations(quiz_version_id, language_code, title, description, fallback_alternative_text)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (quiz_version_id, language_code) DO UPDATE SET
                title = EXCLUDED.title, description = EXCLUDED.description,
                fallback_alternative_text = EXCLUDED.fallback_alternative_text
                """, versionId, languageCode, translation.title(), translation.description(), translation.fallbackAlternativeText());
        for (QuizTranslation.QuestionTranslation question : translation.questions()) {
            if (!question.prompt().isBlank()) jdbcTemplate.update("""
                    INSERT INTO quiz_question_translations(question_id, language_code, prompt, visual_alternative_text, accessible_prompt)
                    VALUES (?, ?, ?, ?, ?) ON CONFLICT (question_id, language_code) DO UPDATE SET prompt = EXCLUDED.prompt,
                    visual_alternative_text = EXCLUDED.visual_alternative_text, accessible_prompt = EXCLUDED.accessible_prompt
                    """, question.questionId(), languageCode, question.prompt(), question.visualAlternativeText(), question.accessiblePrompt());
            for (QuizTranslation.OptionTranslation option : question.answerOptions()) {
                if (!option.text().isBlank()) jdbcTemplate.update("""
                        INSERT INTO quiz_answer_option_translations(answer_option_id, language_code, option_text)
                        VALUES (?, ?, ?) ON CONFLICT (answer_option_id, language_code) DO UPDATE SET option_text = EXCLUDED.option_text
                        """, option.optionId(), languageCode, option.text());
            }
        }
    }

    private static final class MutableQuestion {
        private final UUID id; private final String prompt; private final String visualAlternativeText; private final String accessiblePrompt;
        private final List<QuizTranslation.OptionTranslation> options = new ArrayList<>();
        private MutableQuestion(UUID id, String prompt, String visualAlternativeText, String accessiblePrompt) {
            this.id = id; this.prompt = prompt; this.visualAlternativeText = visualAlternativeText; this.accessiblePrompt = accessiblePrompt;
        }
        private QuizTranslation.QuestionTranslation freeze() { return new QuizTranslation.QuestionTranslation(
                id, prompt, visualAlternativeText, accessiblePrompt, List.copyOf(options)); }
    }

    private static final class MutableQuizTranslation {
        private final String title;
        private final String description;
        private final String fallbackAlternativeText;
        private final Map<UUID, MutableQuestion> questions = new LinkedHashMap<>();

        private MutableQuizTranslation(String title, String description, String fallbackAlternativeText) {
            this.title = title;
            this.description = description;
            this.fallbackAlternativeText = fallbackAlternativeText;
        }

        private QuizTranslation freeze() {
            return new QuizTranslation(title, description, fallbackAlternativeText,
                    questions.values().stream().map(MutableQuestion::freeze).toList());
        }
    }
}
