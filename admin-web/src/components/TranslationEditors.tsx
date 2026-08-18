import { useEffect, useState, type FormEvent } from "react";
import type { ContentApi } from "../api/content-api";
import type { QuizApi } from "../api/quiz-api";
import type { Content, ContentTranslation } from "../domain/content";
import type { QuizTranslation, QuizVersion } from "../domain/quiz";
import { useI18n } from "../i18n/I18nContext";

export function ContentTranslationEditor({ content, api, onError }: { content: Content; api: ContentApi; onError: (reason: unknown) => void }) {
  const { t } = useI18n();
  const [value, setValue] = useState<ContentTranslation | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  useEffect(() => { let active = true; api.getTranslation(content.id).then((result) => { if (active) setValue(result); }).catch(onError); return () => { active = false; }; }, [api, content.id, onError]);
  if (!value) return <p>{t("admin.translationLoading")}</p>;
  const updateSeason = (seasonIndex: number, title: string) => setValue((current) => current && ({ ...current, seasons: current.seasons.map((season, index) => index === seasonIndex ? { ...season, title } : season) }));
  const updateEpisode = (seasonIndex: number, episodeIndex: number, field: "title" | "description", text: string) => setValue((current) => current && ({ ...current, seasons: current.seasons.map((season, index) => index !== seasonIndex ? season : { ...season, episodes: season.episodes.map((episode, itemIndex) => itemIndex === episodeIndex ? { ...episode, [field]: text } : episode) }) }));
  const submit = async (event: FormEvent) => { event.preventDefault(); setIsSaving(true); try { setValue(await api.saveTranslation(content.id, value)); } catch (reason) { onError(reason); } finally { setIsSaving(false); } };
  return <details className="translation-editor"><summary>🇬🇧 {t("admin.englishTranslation")}</summary><form className="stack" onSubmit={submit}>
    <p className="field-help">{t("admin.translationHelp")}</p>
    <label>{t("admin.englishTitle")}<input required maxLength={200} value={value.title} onChange={(event) => setValue({ ...value, title: event.target.value })} /></label>
    <label>{t("admin.englishDescription")}<textarea rows={4} maxLength={2000} value={value.description ?? ""} onChange={(event) => setValue({ ...value, description: event.target.value })} /></label>
    {content.coverMediaId && <label>{t("admin.englishCoverAlt")}<input maxLength={500} value={value.coverAlternativeText ?? ""} onChange={(event) => setValue({ ...value, coverAlternativeText: event.target.value })} /></label>}
    {value.seasons.map((season, seasonIndex) => <fieldset key={season.seasonId}><legend>{t("admin.seasonNumber", { number: content.seasons[seasonIndex]?.seasonNumber ?? seasonIndex + 1 })}</legend>
      <label>{t("admin.englishTitle")}<input required maxLength={200} value={season.title} onChange={(event) => updateSeason(seasonIndex, event.target.value)} /></label>
      {season.episodes.map((episode, episodeIndex) => <div className="translation-subgroup" key={episode.episodeId}><strong>{t("admin.episodeName", { number: content.seasons[seasonIndex]?.episodes[episodeIndex]?.episodeNumber ?? episodeIndex + 1 })}</strong><label>{t("admin.englishTitle")}<input required maxLength={200} value={episode.title} onChange={(event) => updateEpisode(seasonIndex, episodeIndex, "title", event.target.value)} /></label><label>{t("admin.englishDescription")}<textarea rows={2} maxLength={2000} value={episode.description ?? ""} onChange={(event) => updateEpisode(seasonIndex, episodeIndex, "description", event.target.value)} /></label></div>)}
    </fieldset>)}
    <button className="button-primary" disabled={isSaving}>{isSaving ? t("common.saving") : t("admin.saveEnglishTranslation")}</button>
  </form></details>;
}

export function QuizTranslationEditor({ quizId, version, api, onError }: { quizId: string; version: QuizVersion; api: QuizApi; onError: (reason: unknown) => void }) {
  const { t } = useI18n();
  const [value, setValue] = useState<QuizTranslation | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  useEffect(() => { let active = true; api.getTranslation(quizId, version.id).then((result) => { if (active) setValue(result); }).catch(onError); return () => { active = false; }; }, [api, onError, quizId, version.id]);
  if (!value) return <p>{t("admin.translationLoading")}</p>;
  const updateQuestion = (questionIndex: number, field: "prompt" | "visualAlternativeText" | "accessiblePrompt", text: string) => setValue((current) => current && ({ ...current, questions: current.questions.map((question, index) => index === questionIndex ? { ...question, [field]: text } : question) }));
  const updateOption = (questionIndex: number, optionIndex: number, text: string) => setValue((current) => current && ({ ...current, questions: current.questions.map((question, index) => index !== questionIndex ? question : { ...question, answerOptions: question.answerOptions.map((option, itemIndex) => itemIndex === optionIndex ? { ...option, text } : option) }) }));
  const submit = async (event: FormEvent) => { event.preventDefault(); setIsSaving(true); try { setValue(await api.saveTranslation(quizId, version.id, value)); } catch (reason) { onError(reason); } finally { setIsSaving(false); } };
  return <details className="translation-editor"><summary>🇬🇧 {t("admin.englishTranslation")}</summary><form className="stack" onSubmit={submit}>
    <p className="field-help">{t("admin.quizTranslationHelp")}</p>
    <label>{t("admin.englishQuizTitle")}<input required maxLength={200} value={value.title} onChange={(event) => setValue({ ...value, title: event.target.value })} /></label>
    <label>{t("admin.englishDescription")}<textarea rows={3} maxLength={2000} value={value.description ?? ""} onChange={(event) => setValue({ ...value, description: event.target.value })} /></label>
    {value.questions.map((question, questionIndex) => <fieldset key={question.questionId}><legend>{t("admin.questionNumber", { number: version.questions[questionIndex]?.questionOrder ?? questionIndex + 1 })}</legend>
      <label>{t("admin.englishQuestion")}<textarea required rows={3} maxLength={1000} value={question.prompt} onChange={(event) => updateQuestion(questionIndex, "prompt", event.target.value)} /></label>
      {question.answerOptions.map((option, optionIndex) => <label key={option.optionId}>{t("admin.englishOptionNumber", { number: optionIndex + 1 })}<input required maxLength={500} value={option.text} onChange={(event) => updateOption(questionIndex, optionIndex, event.target.value)} /></label>)}
      {version.questions[questionIndex]?.visualRole === "INFORMATIVE" && <><label>{t("admin.englishVisualAlt")}<input maxLength={500} value={question.visualAlternativeText ?? ""} onChange={(event) => updateQuestion(questionIndex, "visualAlternativeText", event.target.value)} /></label><label>{t("admin.englishAccessiblePrompt")}<textarea rows={3} maxLength={1000} value={question.accessiblePrompt ?? ""} onChange={(event) => updateQuestion(questionIndex, "accessiblePrompt", event.target.value)} /></label></>}
    </fieldset>)}
    <button className="button-primary" disabled={isSaving}>{isSaving ? t("common.saving") : t("admin.saveEnglishTranslation")}</button>
  </form></details>;
}
