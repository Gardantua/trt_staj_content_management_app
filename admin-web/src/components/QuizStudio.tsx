import { useEffect, useMemo, useState } from "react";
import type { MediaApi } from "../api/media-api";
import type { QuizApi } from "../api/quiz-api";
import type { Content } from "../domain/content";
import type { QuestionInput, Quiz, QuizQuestion, QuizScopeType, QuizSummary, QuizVersion, VisualRole } from "../domain/quiz";
import { downloadQuizPdf } from "./quiz-pdf";
import { translate, useI18n } from "../i18n/I18nContext";
import type { Language } from "../i18n/types";
import { QuizTranslationEditor } from "./TranslationEditors";

interface QuizStudioProps {
  content: Content;
  quizApi: QuizApi;
  mediaApi: MediaApi;
  initialQuizId?: string | null;
  onError: (reason: unknown) => void;
}

export function QuizStudio({ content, quizApi, mediaApi, initialQuizId = null, onError }: QuizStudioProps) {
  const { language, t } = useI18n();
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [selectedQuizId, setSelectedQuizId] = useState<string | null>(initialQuizId);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    quizApi.listForContent(content.id).then((items) => {
      if (active) setQuizzes(items.filter((item) => item.status !== "ARCHIVED"));
    }).catch(onError).finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; };
  }, [content.id, onError, quizApi, reloadVersion]);

  function refresh(quizId?: string) {
    if (quizId) setSelectedQuizId(quizId);
    setReloadVersion((version) => version + 1);
  }

  return <section className="quiz-studio" aria-labelledby="quiz-studio-heading">
    <div className="studio-section-heading">
      <div><p className="eyebrow">{t("admin.quizAuthoring")}</p><h2 id="quiz-studio-heading">{t("admin.quizCreateEdit")}</h2><p>{t("admin.quizStudioIntro")}</p></div>
      <p className="studio-count">{t("viewer.quizCount", { count: quizzes.length })}</p>
    </div>
    <CreateQuizForm content={content} quizApi={quizApi} onCreated={(quiz) => refresh(quiz.id)} onError={onError} />
    {isLoading ? <p aria-live="polite">{t("viewer.quizzesLoading")}</p> : null}
    {quizzes.length > 0 ? <ul className="quiz-admin-list">
      {quizzes.map((quiz) => <li key={quiz.id}><button type="button" className={selectedQuizId === quiz.id ? "quiz-summary is-selected" : "quiz-summary"} onClick={() => setSelectedQuizId(quiz.id)}>
        <span><small>{scopeLabel(quiz, content, language)}</small><strong>{quiz.title}</strong></span>
        <span><small>{t(quiz.status === "PUBLISHED" ? "admin.available" : "admin.notPublished")}</small><strong>{t("common.questions", { count: quiz.questionCount })}</strong></span>
      </button></li>)}
    </ul> : !isLoading ? <p className="notice">{t("admin.noContentQuiz")}</p> : null}
    {selectedQuizId ? <QuizWorkspace key={`${selectedQuizId}-${reloadVersion}`} content={content} quizNumber={quizzes.findIndex((quiz) => quiz.id === selectedQuizId) + 1} quizId={selectedQuizId} quizApi={quizApi} mediaApi={mediaApi} onChanged={() => refresh(selectedQuizId)} onDeleted={() => { setSelectedQuizId(null); setReloadVersion((version) => version + 1); }} onError={onError} /> : null}
  </section>;
}

function CreateQuizForm({ content, quizApi, onCreated, onError }: { content: Content; quizApi: QuizApi; onCreated: (quiz: Quiz) => void; onError: (reason: unknown) => void }) {
  const { t } = useI18n();
  const [scopeType, setScopeType] = useState<QuizScopeType>("CONTENT");
  const [seasonId, setSeasonId] = useState("");
  const [episodeId, setEpisodeId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const selectedSeason = content.seasons.find((season) => season.id === seasonId);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      onCreated(await quizApi.create({ contentId: content.id, scopeType, seasonId: scopeType === "CONTENT" ? null : seasonId, episodeId: scopeType === "EPISODE" ? episodeId : null, title: title.trim(), description: description.trim() }));
      setTitle(""); setDescription("");
    } catch (reason) { onError(reason); } finally { setIsSaving(false); }
  }

  return <details className="quiz-create-panel"><summary>{t("admin.createQuiz")}</summary><form className="stack" onSubmit={submit}>
    <div className="scope-grid">
      <label>{t("admin.scope")}<select value={scopeType} onChange={(event) => { const next = event.target.value as QuizScopeType; setScopeType(next); setSeasonId(""); setEpisodeId(""); }}><option value="CONTENT">{t("admin.generalScope")}</option>{content.contentType === "SERIES" ? <><option value="SEASON">{t("admin.specificSeason")}</option><option value="EPISODE">{t("admin.specificEpisode")}</option></> : null}</select></label>
      {scopeType !== "CONTENT" ? <label>{t("admin.season")}<select required value={seasonId} onChange={(event) => { setSeasonId(event.target.value); setEpisodeId(""); }}><option value="">{t("admin.selectSeason")}</option>{content.seasons.map((season) => <option key={season.id} value={season.id}>{t("admin.seasonNumber", { number: season.seasonNumber })} · {season.title}</option>)}</select></label> : null}
      {scopeType === "EPISODE" ? <label>{t("admin.episode")}<select required value={episodeId} disabled={!selectedSeason} onChange={(event) => setEpisodeId(event.target.value)}><option value="">{t("admin.selectEpisode")}</option>{selectedSeason?.episodes.map((episode) => <option key={episode.id} value={episode.id}>{t("admin.episodeName", { number: episode.episodeNumber })} · {episode.title}</option>)}</select></label> : null}
    </div>
    <label>{t("admin.quizTitle")}<input required maxLength={200} value={title} onChange={(event) => setTitle(event.target.value)} placeholder={t("admin.quizTitlePlaceholder")} /></label>
    <label>{t("admin.shortDescription")}<textarea rows={3} maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
    <button className="button-primary" disabled={isSaving || !title.trim()}>{isSaving ? t("admin.creating") : t("admin.createQuizAction")}</button>
  </form></details>;
}

function QuizWorkspace({ content, quizNumber, quizId, quizApi, mediaApi, onChanged, onDeleted, onError }: { content: Content; quizNumber: number; quizId: string; quizApi: QuizApi; mediaApi: MediaApi; onChanged: () => void; onDeleted: () => void; onError: (reason: unknown) => void }) {
  const { language, t } = useI18n();
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { let active = true; quizApi.get(quizId).then((result) => { if (active) setQuiz(result); }).catch(onError).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [onError, quizApi, quizId]);
  if (isLoading) return <p aria-live="polite">{t("admin.openingQuiz")}</p>;
  if (!quiz) return null;
  const workingVersion = quiz.versions.find((version) => version.status === "DRAFT");
  const latest = [...quiz.versions].sort((left, right) => right.versionNumber - left.versionNumber)[0];
  const hasPublicationHistory = quiz.versions.some((version) => version.status !== "DRAFT");
  return <section className="quiz-workspace">
    <div className="question-actions"><button type="button" className="button-danger" onClick={async () => { const confirmation = t(hasPublicationHistory ? "admin.retireQuizConfirm" : "admin.deleteDraftQuizConfirm"); if (!window.confirm(confirmation)) return; try { if (hasPublicationHistory) await quizApi.retireQuiz(quiz.id); else await quizApi.deleteQuiz(quiz.id); onDeleted(); } catch (reason) { onError(reason); } }}>{t(hasPublicationHistory ? "admin.retireQuiz" : "admin.deleteQuiz")}</button></div>
    {workingVersion ? <QuizEditor content={content} quizNumber={quizNumber} quiz={quiz} version={workingVersion} quizApi={quizApi} mediaApi={mediaApi} onQuizChanged={(updated) => { setQuiz(updated); onChanged(); }} onError={onError} /> : <div className="notice"><p>{t("admin.publishedQuizEditNotice")}</p><div className="question-actions"><button type="button" className="button-secondary" onClick={async () => { try { await downloadQuizPdf(content, quiz, latest, quizNumber, mediaApi, language); } catch (reason) { onError(reason); } }}>{t("admin.downloadVisualAnswerKey")}</button><button type="button" className="button-primary" onClick={async () => { try { const updated = await quizApi.startEditing(quiz.id); setQuiz(updated); onChanged(); } catch (reason) { onError(reason); } }}>{t("admin.startEditing")}</button></div></div>}
    {latest ? <QuizTranslationEditor quizId={quiz.id} version={latest} api={quizApi} onError={onError} /> : null}
  </section>;
}

function QuizEditor({ content, quizNumber, quiz, version, quizApi, mediaApi, onQuizChanged, onError }: { content: Content; quizNumber: number; quiz: Quiz; version: QuizVersion; quizApi: QuizApi; mediaApi: MediaApi; onQuizChanged: (quiz: Quiz) => void; onError: (reason: unknown) => void }) {
  const { language, t } = useI18n();
  const [title, setTitle] = useState(version.title);
  const [description, setDescription] = useState(version.description ?? "");
  const [showNewQuestion, setShowNewQuestion] = useState(false);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(Math.max(0, version.questions.length - 1));
  const [isQuestionDirty, setIsQuestionDirty] = useState(false);
  const questions = [...version.questions].sort((left, right) => left.questionOrder - right.questionOrder);
  const activeQuestion = questions[activeQuestionIndex] ?? null;
  const confirmQuestionChange = () => !isQuestionDirty || window.confirm(t("admin.unsavedQuestionConfirm"));
  const openQuestion = (index: number) => {
    if ((!showNewQuestion && index === activeQuestionIndex) || !confirmQuestionChange()) return;
    setShowNewQuestion(false);
    setActiveQuestionIndex(index);
  };
  const openNewQuestion = () => {
    if (showNewQuestion || !confirmQuestionChange()) return;
    setShowNewQuestion(true);
  };
  useEffect(() => {
    if (!isQuestionDirty) return;
    const warnAboutIncompleteQuiz = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", warnAboutIncompleteQuiz);
    return () => window.removeEventListener("beforeunload", warnAboutIncompleteQuiz);
  }, [isQuestionDirty]);
  return <>
    <div className="version-heading"><div><p className="eyebrow">{t("admin.quizEditing")}</p><h3>{version.title}</h3></div></div>
    <form className="version-form" onSubmit={async (event) => { event.preventDefault(); try { onQuizChanged(await quizApi.updateVersion(quiz.id, version.id, { title: title.trim(), description: description.trim() })); } catch (reason) { onError(reason); } }}>
      <label>{t("common.title")}<input required maxLength={200} value={title} onChange={(event) => setTitle(event.target.value)} /></label>
      <label>{t("common.description")}<textarea rows={3} maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
      <button className="button-secondary">{t("admin.saveQuizInformation")}</button>
    </form>
    <div className="question-list-heading"><div><h3>{t("admin.questions")}</h3><p>{t("admin.questionsHelp")}</p></div></div>
    <nav className="question-carousel" aria-label={t("admin.questionWorkspace")}><button type="button" disabled={showNewQuestion || activeQuestionIndex === 0} onClick={() => openQuestion(activeQuestionIndex - 1)}>←</button><div>{questions.map((question, index) => <button type="button" key={question.id} className={!showNewQuestion && index === activeQuestionIndex ? "is-active" : ""} onClick={() => openQuestion(index)}>{t("admin.questionNumber", { number: question.questionOrder })}</button>)}<button type="button" aria-label={t("admin.addNewQuestionAria")} className={showNewQuestion ? "is-active" : ""} onClick={openNewQuestion}>+</button></div><button type="button" disabled={showNewQuestion || activeQuestionIndex >= questions.length - 1} onClick={() => openQuestion(activeQuestionIndex + 1)}>→</button></nav>
    {showNewQuestion ? <QuestionForm key={`new-question-${questions.length + 1}`} defaultOrder={questions.length + 1} mediaApi={mediaApi} submitLabel={t("admin.addQuestion")} onDirtyChange={setIsQuestionDirty} onSubmit={async (input) => { const updated = await quizApi.addQuestion(quiz.id, version.id, input); onQuizChanged(updated); setShowNewQuestion(false); setActiveQuestionIndex(updated.versions.find((item) => item.id === version.id)!.questions.length - 1); }} onError={onError} /> : activeQuestion ? <QuestionForm key={activeQuestion.id} question={activeQuestion} defaultOrder={activeQuestion.questionOrder} mediaApi={mediaApi} submitLabel={t("admin.saveChange")} onDirtyChange={setIsQuestionDirty} onSubmit={async (input) => onQuizChanged(await quizApi.updateQuestion(quiz.id, version.id, activeQuestion.id, input))} onDelete={async () => { onQuizChanged(await quizApi.deleteQuestion(quiz.id, version.id, activeQuestion.id)); setActiveQuestionIndex((index) => Math.max(0, index - 1)); }} onError={onError} /> : <p className="notice">{t("admin.firstQuestionHelp")}</p>}
    <div className="publish-panel"><p>{t(content.publicationStatus === "DRAFT" ? "admin.draftPublishHelp" : "admin.publishedPublishHelp")}</p><div className="question-actions"><button type="button" className="button-secondary" disabled={questions.length === 0} onClick={async () => { try { await downloadQuizPdf(content, quiz, version, quizNumber, mediaApi, language); } catch (reason) { onError(reason); } }}>{t("admin.downloadVisualAnswerKey")}</button><button type="button" className="button-primary" disabled={questions.length === 0 || content.publicationStatus === "DRAFT"} onClick={async () => { if (!window.confirm(t("admin.publishQuizConfirm"))) return; try { onQuizChanged(await quizApi.publish(quiz.id, version.id)); } catch (reason) { onError(reason); } }}>{t(content.publicationStatus === "DRAFT" ? "admin.addContentFirst" : "admin.publishQuiz")}</button></div></div>
  </>;
}

function QuestionForm({ question, defaultOrder, mediaApi, submitLabel, onSubmit, onDelete, onDirtyChange, onError }: { question?: QuizQuestion; defaultOrder: number; mediaApi: MediaApi; submitLabel: string; onSubmit: (input: QuestionInput) => Promise<void>; onDelete?: () => Promise<void>; onDirtyChange: (isDirty: boolean) => void; onError: (reason: unknown) => void }) {
  const { t } = useI18n();
  const questionOrder = question?.questionOrder ?? defaultOrder;
  const [prompt, setPrompt] = useState(question?.prompt ?? "");
  const [hasVisual, setHasVisual] = useState(Boolean(question?.visualMediaId));
  const [visualMediaId, setVisualMediaId] = useState<string | null>(question?.visualMediaId ?? null);
  const [visualRole, setVisualRole] = useState<VisualRole>(question?.visualRole ?? "DECORATIVE");
  const [alternativeText, setAlternativeText] = useState(question?.visualAlternativeText ?? "");
  const [accessiblePrompt, setAccessiblePrompt] = useState(question?.accessiblePrompt ?? "");
  const [options, setOptions] = useState(() => question?.answerOptions.map((option) => ({ text: option.text, correct: option.correct })) ?? [{ text: "", correct: true }, { text: "", correct: false }, { text: "", correct: false }, { text: "", correct: false }]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isBusy, setIsBusy] = useState(false);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const correctIndex = Math.max(0, options.findIndex((option) => option.correct));
  const input = useMemo<QuestionInput>(() => ({ questionOrder, prompt: prompt.trim(), visualMediaId: hasVisual ? visualMediaId : null, visualRole: hasVisual && visualMediaId ? visualRole : null, visualAlternativeText: hasVisual && visualMediaId && visualRole === "INFORMATIVE" ? alternativeText.trim() : null, accessiblePrompt: hasVisual && visualMediaId && visualRole === "INFORMATIVE" ? accessiblePrompt.trim() : null, answerOptions: options.map((option, index) => ({ optionOrder: index + 1, text: option.text.trim(), correct: index === correctIndex })) }), [accessiblePrompt, alternativeText, correctIndex, hasVisual, options, prompt, questionOrder, visualMediaId, visualRole]);
  const [savedInput, setSavedInput] = useState(() => JSON.stringify(input));
  useEffect(() => {
    onDirtyChange(JSON.stringify(input) !== savedInput || selectedFile !== null);
  }, [input, onDirtyChange, savedInput, selectedFile]);
  useEffect(() => () => onDirtyChange(false), [onDirtyChange]);

  return <form className="question-form stack" onSubmit={async (event) => { event.preventDefault(); setValidationMessage(null); if (hasVisual && !visualMediaId) { setValidationMessage(t("admin.visualRequired")); return; } setIsBusy(true); try { await onSubmit(input); setSavedInput(JSON.stringify(input)); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>
    <p className="field-help">{t("admin.difficultyHelp")}</p>
    <label>{t("admin.questionText")}<textarea required rows={3} maxLength={1000} value={prompt} onChange={(event) => setPrompt(event.target.value)} /></label>
    <fieldset className="option-fieldset"><legend>{t("admin.fourOptions")}</legend>{options.map((option, index) => <div className="option-row" key={index}><input type="radio" name={`correct-${question?.id ?? "new"}`} aria-label={t("admin.makeOptionCorrectAria", { number: index + 1 })} checked={index === correctIndex} onChange={() => setOptions((current) => current.map((item, itemIndex) => ({ ...item, correct: itemIndex === index })))} /><label>{t("admin.optionNumber", { number: index + 1 })}<input required maxLength={500} value={option.text} onChange={(event) => setOptions((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, text: event.target.value } : item))} /></label></div>)}</fieldset>
    <label className="checkbox-label"><input type="checkbox" checked={hasVisual} onChange={(event) => { setHasVisual(event.target.checked); if (!event.target.checked) setVisualMediaId(null); }} />{t("admin.useQuestionVisual")}</label>
    {hasVisual ? <div className="visual-editor"><div className="upload-inline"><label>{t("admin.newImage")}<input type="file" accept="image/jpeg,image/png" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} /></label><button type="button" className="button-secondary" disabled={!selectedFile || isBusy} onClick={async () => { if (!selectedFile) return; setIsBusy(true); try { const asset = await mediaApi.uploadImage(selectedFile); setVisualMediaId(asset.id); setSelectedFile(null); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>{t("admin.uploadAndSelect")}</button></div>
      <label>{t("admin.visualRole")}<select value={visualRole} onChange={(event) => setVisualRole(event.target.value as VisualRole)}><option value="DECORATIVE">{t("admin.decorative")}</option><option value="INFORMATIVE">{t("admin.informative")}</option></select></label>
      {visualRole === "INFORMATIVE" ? <><label>{t("admin.alternativeText")}<input required maxLength={500} value={alternativeText} onChange={(event) => setAlternativeText(event.target.value)} placeholder={t("admin.visualAltPlaceholder")} /></label><label>{t("admin.accessibleQuestion")}<textarea required rows={3} maxLength={1000} value={accessiblePrompt} onChange={(event) => setAccessiblePrompt(event.target.value)} placeholder={t("admin.accessibleQuestionPlaceholder")} /></label><p className="field-help">{t("admin.answerLeakWarning")}</p></> : <p className="field-help">{t("admin.decorativeHelp")}</p>}</div> : null}
    {validationMessage ? <p className="form-validation" role="alert">{validationMessage}</p> : null}
    <div className="question-actions"><button className="button-primary" disabled={isBusy}>{isBusy ? t("common.saving") : submitLabel}</button>{onDelete ? <button type="button" className="text-danger" disabled={isBusy} onClick={async () => { setIsBusy(true); try { await onDelete(); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>{t("admin.deleteQuestion")}</button> : null}</div>
  </form>;
}

function scopeLabel(quiz: Pick<QuizSummary, "scopeType" | "seasonId" | "episodeId">, content: Content, language: Language) {
  if (quiz.scopeType === "CONTENT") return translate(language, content.contentType === "SERIES" ? "admin.seriesGeneral" : "admin.filmGeneral");
  const season = content.seasons.find((item) => item.id === quiz.seasonId);
  if (quiz.scopeType === "SEASON") return season ? translate(language, "viewer.season", { number: season.seasonNumber }) : translate(language, "viewer.seasonQuiz");
  const episode = season?.episodes.find((item) => item.id === quiz.episodeId);
  return season && episode ? translate(language, "viewer.episode", { season: season.seasonNumber, episode: episode.episodeNumber }) : translate(language, "viewer.episodeQuiz");
}
