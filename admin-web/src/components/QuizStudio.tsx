import { useEffect, useMemo, useState } from "react";
import type { MediaApi } from "../api/media-api";
import type { QuizApi } from "../api/quiz-api";
import type { Content } from "../domain/content";
import type { QuestionInput, Quiz, QuizQuestion, QuizScopeType, QuizSummary, QuizVersion, VisualRole } from "../domain/quiz";
import { downloadQuizPdf } from "./quiz-pdf";

interface QuizStudioProps {
  content: Content;
  quizApi: QuizApi;
  mediaApi: MediaApi;
  initialQuizId?: string | null;
  onError: (reason: unknown) => void;
}

export function QuizStudio({ content, quizApi, mediaApi, initialQuizId = null, onError }: QuizStudioProps) {
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
      <div><p className="eyebrow">Quiz yazarlığı</p><h2 id="quiz-studio-heading">Quiz oluşturma ve düzenleme</h2><p>Dizi geneli, sezon veya bölüm kapsamını seçin; soruları ve cevapları hazırlayın.</p></div>
      <p className="studio-count">{quizzes.length} quiz</p>
    </div>
    <CreateQuizForm content={content} quizApi={quizApi} onCreated={(quiz) => refresh(quiz.id)} onError={onError} />
    {isLoading ? <p aria-live="polite">Quizler yükleniyor…</p> : null}
    {quizzes.length > 0 ? <ul className="quiz-admin-list">
      {quizzes.map((quiz) => <li key={quiz.id}><button type="button" className={selectedQuizId === quiz.id ? "quiz-summary is-selected" : "quiz-summary"} onClick={() => setSelectedQuizId(quiz.id)}>
        <span><small>{scopeLabel(quiz, content)}</small><strong>{quiz.title}</strong></span>
        <span><small>{quiz.status === "PUBLISHED" ? "Kullanıma açık" : "Henüz yayınlanmadı"}</small><strong>{quiz.questionCount} soru</strong></span>
      </button></li>)}
    </ul> : !isLoading ? <p className="notice">Bu içerik için henüz quiz yok. İlk quizi yukarıdaki formdan oluşturun.</p> : null}
    {selectedQuizId ? <QuizWorkspace key={`${selectedQuizId}-${reloadVersion}`} content={content} quizNumber={quizzes.findIndex((quiz) => quiz.id === selectedQuizId) + 1} quizId={selectedQuizId} quizApi={quizApi} mediaApi={mediaApi} onChanged={() => refresh(selectedQuizId)} onDeleted={() => { setSelectedQuizId(null); setReloadVersion((version) => version + 1); }} onError={onError} /> : null}
  </section>;
}

function CreateQuizForm({ content, quizApi, onCreated, onError }: { content: Content; quizApi: QuizApi; onCreated: (quiz: Quiz) => void; onError: (reason: unknown) => void }) {
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

  return <details className="quiz-create-panel"><summary>Yeni quiz oluştur</summary><form className="stack" onSubmit={submit}>
    <div className="scope-grid">
      <label>Kapsam<select value={scopeType} onChange={(event) => { const next = event.target.value as QuizScopeType; setScopeType(next); setSeasonId(""); setEpisodeId(""); }}><option value="CONTENT">İçerik geneli</option>{content.contentType === "SERIES" ? <><option value="SEASON">Belirli sezon</option><option value="EPISODE">Belirli bölüm</option></> : null}</select></label>
      {scopeType !== "CONTENT" ? <label>Sezon<select required value={seasonId} onChange={(event) => { setSeasonId(event.target.value); setEpisodeId(""); }}><option value="">Sezon seçin</option>{content.seasons.map((season) => <option key={season.id} value={season.id}>{season.seasonNumber}. sezon · {season.title}</option>)}</select></label> : null}
      {scopeType === "EPISODE" ? <label>Bölüm<select required value={episodeId} disabled={!selectedSeason} onChange={(event) => setEpisodeId(event.target.value)}><option value="">Bölüm seçin</option>{selectedSeason?.episodes.map((episode) => <option key={episode.id} value={episode.id}>{episode.episodeNumber}. bölüm · {episode.title}</option>)}</select></label> : null}
    </div>
    <label>Quiz başlığı<input required maxLength={200} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Örn. Sezon finalini ne kadar hatırlıyorsun?" /></label>
    <label>Kısa açıklama<textarea rows={3} maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
    <button className="button-primary" disabled={isSaving || !title.trim()}>{isSaving ? "Oluşturuluyor…" : "Quizi oluştur"}</button>
  </form></details>;
}

function QuizWorkspace({ content, quizNumber, quizId, quizApi, mediaApi, onChanged, onDeleted, onError }: { content: Content; quizNumber: number; quizId: string; quizApi: QuizApi; mediaApi: MediaApi; onChanged: () => void; onDeleted: () => void; onError: (reason: unknown) => void }) {
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { let active = true; quizApi.get(quizId).then((result) => { if (active) setQuiz(result); }).catch(onError).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [onError, quizApi, quizId]);
  if (isLoading) return <p aria-live="polite">Quiz açılıyor…</p>;
  if (!quiz) return null;
  const workingVersion = quiz.versions.find((version) => version.status === "DRAFT");
  const latest = [...quiz.versions].sort((left, right) => right.versionNumber - left.versionNumber)[0];
  const hasPublicationHistory = quiz.versions.some((version) => version.status !== "DRAFT");
  return <section className="quiz-workspace">
    <div className="question-actions"><button type="button" className="button-danger" onClick={async () => { const confirmation = hasPublicationHistory ? "Bu quiz aktif kullanımdan kaldırılıp Quiz geçmişi sayfasına taşınacak. Kazanılmış XP korunacak. Devam edilsin mi?" : "Bu quiz henüz kullanıma açılmadığı için kalıcı olarak silinecek. Devam edilsin mi?"; if (!window.confirm(confirmation)) return; try { if (hasPublicationHistory) await quizApi.retireQuiz(quiz.id); else await quizApi.deleteQuiz(quiz.id); onDeleted(); } catch (reason) { onError(reason); } }}>{hasPublicationHistory ? "Quiz'i geçmişe kaldır" : "Quiz'i kalıcı sil"}</button></div>
    {workingVersion ? <QuizEditor content={content} quizNumber={quizNumber} quiz={quiz} version={workingVersion} quizApi={quizApi} mediaApi={mediaApi} onQuizChanged={(updated) => { setQuiz(updated); onChanged(); }} onError={onError} /> : <div className="notice"><p>Bu quiz kullanıma açık. Değişiklik yapmak için düzenlemeyi başlatın; mevcut kullanıcı sonuçları korunur.</p><div className="question-actions"><button type="button" className="button-secondary" onClick={async () => { try { await downloadQuizPdf(content, quiz, latest, quizNumber, mediaApi); } catch (reason) { onError(reason); } }}>Görselli cevap anahtarı PDF indir</button><button type="button" className="button-primary" onClick={async () => { try { const updated = await quizApi.startEditing(quiz.id); setQuiz(updated); onChanged(); } catch (reason) { onError(reason); } }}>Düzenlemeye başla</button></div></div>}
  </section>;
}

function QuizEditor({ content, quizNumber, quiz, version, quizApi, mediaApi, onQuizChanged, onError }: { content: Content; quizNumber: number; quiz: Quiz; version: QuizVersion; quizApi: QuizApi; mediaApi: MediaApi; onQuizChanged: (quiz: Quiz) => void; onError: (reason: unknown) => void }) {
  const [title, setTitle] = useState(version.title);
  const [description, setDescription] = useState(version.description ?? "");
  const [showNewQuestion, setShowNewQuestion] = useState(false);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(Math.max(0, version.questions.length - 1));
  const [isQuestionDirty, setIsQuestionDirty] = useState(false);
  const questions = [...version.questions].sort((left, right) => left.questionOrder - right.questionOrder);
  const activeQuestion = questions[activeQuestionIndex] ?? null;
  const confirmQuestionChange = () => !isQuestionDirty || window.confirm("Bu soruda kaydedilmemiş değişiklikler var. Başka soruya geçerseniz değişiklikler kaybolacak. Devam edilsin mi?");
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
    <div className="version-heading"><div><p className="eyebrow">Quiz düzenleme</p><h3>{version.title}</h3></div></div>
    <form className="version-form" onSubmit={async (event) => { event.preventDefault(); try { onQuizChanged(await quizApi.updateVersion(quiz.id, version.id, { title: title.trim(), description: description.trim() })); } catch (reason) { onError(reason); } }}>
      <label>Başlık<input required maxLength={200} value={title} onChange={(event) => setTitle(event.target.value)} /></label>
      <label>Açıklama<textarea rows={3} maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
      <button className="button-secondary">Quiz bilgilerini kaydet</button>
    </form>
    <div className="question-list-heading"><div><h3>Sorular</h3><p>Soru sayısı serbesttir. Her soru dört şıklı ve 30 saniyedir; sekmelerden sorular arasında geçebilirsiniz.</p></div></div>
    <nav className="question-carousel" aria-label="Soru çalışma alanı"><button type="button" disabled={showNewQuestion || activeQuestionIndex === 0} onClick={() => openQuestion(activeQuestionIndex - 1)}>←</button><div>{questions.map((question, index) => <button type="button" key={question.id} className={!showNewQuestion && index === activeQuestionIndex ? "is-active" : ""} onClick={() => openQuestion(index)}>Soru {question.questionOrder}</button>)}<button type="button" aria-label="Yeni soru ekle" className={showNewQuestion ? "is-active" : ""} onClick={openNewQuestion}>+</button></div><button type="button" disabled={showNewQuestion || activeQuestionIndex >= questions.length - 1} onClick={() => openQuestion(activeQuestionIndex + 1)}>→</button></nav>
    {showNewQuestion ? <QuestionForm key={`new-question-${questions.length + 1}`} defaultOrder={questions.length + 1} mediaApi={mediaApi} submitLabel="Soruyu ekle" onDirtyChange={setIsQuestionDirty} onSubmit={async (input) => { const updated = await quizApi.addQuestion(quiz.id, version.id, input); onQuizChanged(updated); setShowNewQuestion(false); setActiveQuestionIndex(updated.versions.find((item) => item.id === version.id)!.questions.length - 1); }} onError={onError} /> : activeQuestion ? <QuestionForm key={activeQuestion.id} question={activeQuestion} defaultOrder={activeQuestion.questionOrder} mediaApi={mediaApi} submitLabel="Değişikliği kaydet" onDirtyChange={setIsQuestionDirty} onSubmit={async (input) => onQuizChanged(await quizApi.updateQuestion(quiz.id, version.id, activeQuestion.id, input))} onDelete={async () => { onQuizChanged(await quizApi.deleteQuestion(quiz.id, version.id, activeQuestion.id)); setActiveQuestionIndex((index) => Math.max(0, index - 1)); }} onError={onError} /> : <p className="notice">İlk soruyu eklemek için + sekmesini seçin.</p>}
    <div className="publish-panel"><p>{content.publicationStatus === "DRAFT" ? "Kullanıcı sayfasında görünmesi için önce İçerik bilgileri sekmesinden diziyi veya filmi kataloğa ekleyin; ardından bu quizi kullanıma açın." : "İçerik katalogda. Sorular hazır olduğunda quizi ayrıca kullanıma açın; sonraki düzenlemelerde geçmiş kullanıcı sonuçları korunur."}</p><div className="question-actions"><button type="button" className="button-secondary" disabled={questions.length === 0} onClick={async () => { try { await downloadQuizPdf(content, quiz, version, quizNumber, mediaApi); } catch (reason) { onError(reason); } }}>Görselli cevap anahtarı PDF indir</button><button type="button" className="button-primary" disabled={questions.length === 0 || content.publicationStatus === "DRAFT"} onClick={async () => { if (!window.confirm("Bu quiz kullanıma açılsın mı?")) return; try { onQuizChanged(await quizApi.publish(quiz.id, version.id)); } catch (reason) { onError(reason); } }}>{content.publicationStatus === "DRAFT" ? "Önce içeriği kataloğa ekleyin" : "Quiz'i kullanıma aç"}</button></div></div>
  </>;
}

function QuestionForm({ question, defaultOrder, mediaApi, submitLabel, onSubmit, onDelete, onDirtyChange, onError }: { question?: QuizQuestion; defaultOrder: number; mediaApi: MediaApi; submitLabel: string; onSubmit: (input: QuestionInput) => Promise<void>; onDelete?: () => Promise<void>; onDirtyChange: (isDirty: boolean) => void; onError: (reason: unknown) => void }) {
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

  return <form className="question-form stack" onSubmit={async (event) => { event.preventDefault(); setValidationMessage(null); if (hasVisual && !visualMediaId) { setValidationMessage("Soruya özel görsel açıksa önce bir görsel yükleyin."); return; } setIsBusy(true); try { await onSubmit(input); setSavedInput(JSON.stringify(input)); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>
    <p className="field-help">Normal zorluk otomatik uygulanır ve kullanıcıya ayrıca gösterilmez.</p>
    <label>Soru metni<textarea required rows={3} maxLength={1000} value={prompt} onChange={(event) => setPrompt(event.target.value)} /></label>
    <fieldset className="option-fieldset"><legend>Dört cevap seçeneği</legend>{options.map((option, index) => <div className="option-row" key={index}><input type="radio" name={`correct-${question?.id ?? "new"}`} aria-label={`${index + 1}. seçeneği doğru cevap yap`} checked={index === correctIndex} onChange={() => setOptions((current) => current.map((item, itemIndex) => ({ ...item, correct: itemIndex === index })))} /><label>{index + 1}. seçenek<input required maxLength={500} value={option.text} onChange={(event) => setOptions((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, text: event.target.value } : item))} /></label></div>)}</fieldset>
    <label className="checkbox-label"><input type="checkbox" checked={hasVisual} onChange={(event) => { setHasVisual(event.target.checked); if (!event.target.checked) setVisualMediaId(null); }} />Soruya özel görsel kullan</label>
    {hasVisual ? <div className="visual-editor"><div className="upload-inline"><label>Yeni görsel<input type="file" accept="image/jpeg,image/png" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} /></label><button type="button" className="button-secondary" disabled={!selectedFile || isBusy} onClick={async () => { if (!selectedFile) return; setIsBusy(true); try { const asset = await mediaApi.uploadImage(selectedFile); setVisualMediaId(asset.id); setSelectedFile(null); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>Yükle ve seç</button></div>
      <label>Görselin rolü<select value={visualRole} onChange={(event) => setVisualRole(event.target.value as VisualRole)}><option value="DECORATIVE">Dekoratif</option><option value="INFORMATIVE">Sorunun bilgisini taşıyor</option></select></label>
      {visualRole === "INFORMATIVE" ? <><label>Alternatif metin<input required maxLength={500} value={alternativeText} onChange={(event) => setAlternativeText(event.target.value)} placeholder="Görselde görünen kişi, mekân ve önemli ayrıntıları kısa biçimde anlatın." /></label><label>Eşdeğer erişilebilir soru<textarea required rows={3} maxLength={1000} value={accessiblePrompt} onChange={(event) => setAccessiblePrompt(event.target.value)} placeholder="Görseli göremeyen kullanıcının aynı soruyu cevaplayabilmesi için, doğru cevabı söylemeden eşdeğer bilgiyi yazın." /></label><p className="field-help">Bu iki metin doğru cevabı açıkça içermemelidir.</p></> : <p className="field-help">Dekoratif görsel cevap için gerekli bilgi taşımaz; kullanıcıya içerik kapağı da fallback olarak gösterilebilir.</p>}</div> : null}
    {validationMessage ? <p className="form-validation" role="alert">{validationMessage}</p> : null}
    <div className="question-actions"><button className="button-primary" disabled={isBusy}>{isBusy ? "Kaydediliyor…" : submitLabel}</button>{onDelete ? <button type="button" className="text-danger" disabled={isBusy} onClick={async () => { setIsBusy(true); try { await onDelete(); } catch (reason) { onError(reason); } finally { setIsBusy(false); } }}>Soruyu sil</button> : null}</div>
  </form>;
}

function scopeLabel(quiz: Pick<QuizSummary, "scopeType" | "seasonId" | "episodeId">, content: Content) {
  if (quiz.scopeType === "CONTENT") return content.contentType === "SERIES" ? "Dizi geneli" : "Film geneli";
  const season = content.seasons.find((item) => item.id === quiz.seasonId);
  if (quiz.scopeType === "SEASON") return season ? `${season.seasonNumber}. sezon` : "Sezon quizi";
  const episode = season?.episodes.find((item) => item.id === quiz.episodeId);
  return season && episode ? `${season.seasonNumber}. sezon · ${episode.episodeNumber}. bölüm` : "Bölüm quizi";
}
