package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.QuizTranslation;
import com.trt.contentengagement.quiz.application.QuizTranslationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuizTranslationRepository implements QuizTranslationRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcQuizTranslationRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public Optional<QuizTranslation> find(UUID versionId, String languageCode) {
        List<Map<String, Object>> roots = jdbcTemplate.queryForList("""
                SELECT title, description, fallback_alternative_text FROM quiz_version_translations
                WHERE quiz_version_id = ? AND language_code = ?
                """, versionId, languageCode);
        if (roots.isEmpty()) return Optional.empty();
        Map<UUID, MutableQuestion> questions = new LinkedHashMap<>();
        jdbcTemplate.queryForList("""
                SELECT q.id question_id, qt.prompt, qt.visual_alternative_text, qt.accessible_prompt
                FROM quiz_questions q JOIN quiz_question_translations qt
                  ON qt.question_id = q.id AND qt.language_code = ?
                WHERE q.quiz_version_id = ? ORDER BY q.question_order
                """, languageCode, versionId).forEach(row -> questions.put((UUID) row.get("question_id"),
                new MutableQuestion((UUID) row.get("question_id"), (String) row.get("prompt"),
                        (String) row.get("visual_alternative_text"), (String) row.get("accessible_prompt"))));
        jdbcTemplate.queryForList("""
                SELECT q.id question_id, o.id option_id, ot.option_text
                FROM quiz_questions q JOIN quiz_answer_options o ON o.question_id = q.id
                JOIN quiz_answer_option_translations ot ON ot.answer_option_id = o.id AND ot.language_code = ?
                WHERE q.quiz_version_id = ? ORDER BY q.question_order, o.option_order
                """, languageCode, versionId).forEach(row -> questions.computeIfAbsent((UUID) row.get("question_id"),
                id -> new MutableQuestion(id, "", null, null)).options.add(
                new QuizTranslation.OptionTranslation((UUID) row.get("option_id"), (String) row.get("option_text"))));
        Map<String, Object> root = roots.getFirst();
        return Optional.of(new QuizTranslation((String) root.get("title"), (String) root.get("description"),
                (String) root.get("fallback_alternative_text"), questions.values().stream().map(MutableQuestion::freeze).toList()));
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
}
