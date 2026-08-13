import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { AuthApi, type AccountSession } from "./auth-api";
import { PublicApi, PublicApiRequestError } from "./public-api";
import type { AnswerFeedback, AnswerSubmissionResult, Leaderboard, LeaderboardEntry, PublicContent, PublicContentPage, PublicContentSummary, PublishedQuiz, PublishedQuizSummary, QuizAttempt, QuizResultSummary, XpSummary } from "./types";
import tabiiLogoUrl from "../../../gorseller/Tabii_-_TRT_Logo.png";
import platformArtworkUrl from "../../../gorseller/tabii-dijital-platform-2041030.jpg";

const PAGE_SIZE = 12;
const RECENT_CONTENT_IDS_KEY = "viewer-recent-content-ids";
type View = "home" | "quizzes" | "profile";
type QuizSort = "CONTENT_TITLE" | "QUIZ_TITLE" | "QUESTION_COUNT";
interface ViewerRoute { contentId: string | null; quizId: string | null; view: View; }

export function UserApp() {
  const authApi = useMemo(() => new AuthApi(), []);
  const api = useMemo(() => new PublicApi(), []);
  const [account, setAccount] = useState<AccountSession | null | undefined>(undefined);
  const [route, setRoute] = useState<ViewerRoute>(readRoute);

  useEffect(() => {
    let active = true;
    authApi.me().then((session) => { if (active) setAccount(session); }).catch(() => { if (active) setAccount(null); });
    return () => { active = false; };
  }, [authApi]);

  useEffect(() => {
    const updateRoute = () => setRoute(readRoute());
    window.addEventListener("popstate", updateRoute);
    return () => window.removeEventListener("popstate", updateRoute);
  }, []);

  function navigate(nextRoute: Partial<ViewerRoute>) {
    const next = { ...route, ...nextRoute };
    const url = new URL(window.location.href);
    setRouteParam(url, "content", next.contentId); setRouteParam(url, "quiz", next.quizId);
    setRouteParam(url, "view", next.view === "home" ? null : next.view);
    window.history.pushState({}, "", url); setRoute(next); window.scrollTo({ top: 0, behavior: "smooth" });
  }

  if (account === undefined) return <main className="viewer-shell sign-in-shell"><p className="loading-copy">Oturum kontrol ediliyor…</p></main>;
  if (!account) return <AccountAccess authApi={authApi} onSignedIn={setAccount} />;
  return <main className="viewer-shell">
    <SiteHeader account={account} view={route.view} onNavigate={(view) => navigate({ view, contentId: null, quizId: null })} onSignOut={async () => { try { await authApi.logout(); } finally { setAccount(null); } }} />
    {route.quizId ? <QuizExperience api={api} quizId={route.quizId} onExit={() => navigate({ quizId: null })} onViewProfile={() => navigate({ quizId: null, contentId: null, view: "profile" })} /> : route.contentId
      ? <ContentDetail api={api} contentId={route.contentId} onBack={() => navigate({ contentId: null })} onStartQuiz={(quizId) => navigate({ quizId })} />
      : route.view === "quizzes" ? <QuizCatalogue api={api} onStartQuiz={(quizId) => navigate({ quizId })} /> : route.view === "profile" ? <Profile api={api} /> : <Catalogue api={api} onOpen={(contentId) => navigate({ contentId })} />}
  </main>;
}

function SiteHeader({ account, view, onNavigate, onSignOut }: { account: AccountSession; view: View; onNavigate: (view: View) => void; onSignOut: () => Promise<void> }) {
  return <header className="viewer-header"><a className="viewer-brand" href="/" onClick={(event) => { event.preventDefault(); onNavigate("home"); }} aria-label="Hikâye İzi ana sayfası"><img src={tabiiLogoUrl} alt="tabii" /><span><strong>Hikâye</strong> İzi</span><em>izle · hatırla · oyna</em></a>
    <nav aria-label="Kullanıcı menüsü"><button className={view === "home" ? "active" : ""} onClick={() => onNavigate("home")}>Keşfet</button><button className={view === "quizzes" ? "active" : ""} onClick={() => onNavigate("quizzes")}>Quizler</button><button className={view === "profile" ? "active" : ""} onClick={() => onNavigate("profile")}>Profil</button></nav><p><span>{account.displayName}</span><button className="sign-out" onClick={() => void onSignOut()}>Çıkış</button></p></header>;
}

function AccountAccess({ authApi, onSignedIn }: { authApi: AuthApi; onSignedIn: (account: AccountSession) => void }) {
  type AccountMode = "login" | "register" | "forgot" | "reset";
  const resetToken = new URLSearchParams(window.location.search).get("resetToken");
  const [mode, setMode] = useState<AccountMode>(resetToken ? "reset" : "login");
  const [email, setEmail] = useState(""); const [displayName, setDisplayName] = useState(""); const [password, setPassword] = useState("");
  const [error, setError] = useState<PublicApiRequestError | null>(null); const [successMessage, setSuccessMessage] = useState<string | null>(null); const [isSubmitting, setIsSubmitting] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null); setSuccessMessage(null); setIsSubmitting(true);
    try {
      if (mode === "forgot") {
        await authApi.requestPasswordReset(email);
        setSuccessMessage("E-posta kayıtlıysa şifre yenileme bağlantısını gönderdik.");
      } else if (mode === "reset" && resetToken) {
        await authApi.resetPassword(resetToken, password);
        window.history.replaceState({}, "", "/");
        setMode("login"); setPassword("");
        setSuccessMessage("Şifren yenilendi. Şimdi giriş yapabilirsin.");
      } else {
        onSignedIn(mode === "login" ? await authApi.login(email, password) : await authApi.register(email, displayName, password));
      }
    }
    catch (reason) { setError(asPublicError(reason)); }
    finally { setIsSubmitting(false); }
  }
  function changeMode(nextMode: AccountMode) { setMode(nextMode); setError(null); setSuccessMessage(null); }
  const isLogin = mode === "login";
  const title = mode === "login" ? "Şimdi giriş yap" : mode === "register" ? "Hesabını oluştur" : mode === "forgot" ? "Şifreni yenile" : "Yeni şifreni belirle";
  const description = mode === "login" ? "Quizlere kaldığın yerden devam et." : mode === "register" ? "İlerlemeni kaydetmek için ücretsiz hesabını oluştur." : mode === "forgot" ? "Hesabındaki e-postayı yaz; sana tek kullanımlık bağlantı gönderelim." : "Yeni şifren en az 8 karakter olmalı.";
  return <main className="viewer-shell sign-in-shell">
    <div className="sign-in-backdrop" aria-hidden="true"><img src={platformArtworkUrl} alt="" /></div>
    <header className="sign-in-header">
      <a className="sign-in-brand" href="/" aria-label="tabii Hikâye İzi ana sayfası"><img className="sign-in-wordmark" src={platformArtworkUrl} alt="tabii" /><span>Hikâye İzi</span></a>
      <button className="auth-switch auth-switch--header" type="button" onClick={() => changeMode(isLogin ? "register" : "login")}>{isLogin ? "Hesap oluştur" : "Giriş yap"}</button>
    </header>
    <section className="local-access" aria-labelledby="account-access-title">
      <p className="kicker">Hikâye İzi</p>
      <h1 id="account-access-title">{title}</h1>
      <p>{description}</p>
      <form onSubmit={submit}>
        {mode === "register" && <label htmlFor="display-name">Görünen ad<input id="display-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} autoComplete="name" minLength={2} maxLength={80} placeholder="Görünen adın" required /></label>}
        {mode !== "reset" && <label htmlFor="account-email">E-posta<input id="account-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" placeholder="ornek@email.com" required /></label>}
        {(mode === "login" || mode === "register" || mode === "reset") && <label htmlFor="account-password">{mode === "reset" ? "Yeni şifre" : "Şifre"}<input id="account-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={8} maxLength={64} placeholder="En az 8 karakter" required /></label>}
        {isLogin && <button className="forgot-password" type="button" onClick={() => changeMode("forgot")}>Şifremi unuttum</button>}
        <button className="auth-submit" disabled={isSubmitting || (mode === "reset" && !resetToken)}>{isSubmitting ? "Bekleyin…" : mode === "login" ? "Giriş yap" : mode === "register" ? "Hesap oluştur" : mode === "forgot" ? "Bağlantıyı gönder" : "Şifremi yenile"}</button>
      </form>
      <ErrorNotice error={error} />
      {successMessage && <p className="auth-success" role="status">{successMessage}</p>}
      <p className="auth-alternative">{isLogin ? "Hesabın yok mu?" : "Giriş ekranına dönmek ister misin?"} <button className="auth-switch" type="button" onClick={() => changeMode(isLogin ? "register" : "login")}>{isLogin ? "Ücretsiz hesap oluştur" : "Giriş yap"}</button></p>
      <p className="sign-in-note">Bu hesaplar yalnız yerel geliştirme içindir; kurumsal tabii hesabı değildir.</p>
    </section>
  </main>;
}

function Catalogue({ api, onOpen }: { api: PublicApi; onOpen: (contentId: string) => void }) {
  const [contents, setContents] = useState<PublicContentSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true); const [currentPage, setCurrentPage] = useState(0); const [searchQuery, setSearchQuery] = useState(""); const [recentContentIds, setRecentContentIds] = useState(readRecentContentIds);
  useEffect(() => { let active = true; setIsLoading(true); setError(null); api.listAllContents().then((result) => { if (active) setContents(result); }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api]);
  const visibleContents = orderContentsByRecentViews(filterContentsBySearch(contents, searchQuery), recentContentIds);
  const page = paginateContents(visibleContents, currentPage, PAGE_SIZE);
  function openContent(contentId: string) { const nextIds = rememberRecentContent(recentContentIds, contentId); setRecentContentIds(nextIds); saveRecentContentIds(nextIds); onOpen(contentId); }
  return <><section className="viewer-hero"><img className="hero-artwork" src={platformArtworkUrl} alt="" /><div className="hero-copy"><p>Hikâye İzi</p><h1>İzlediğin hikâyenin izini sür.</h1><p className="hero-description">Bir sahneyi hatırla, kısa bir meydan okumayı tamamla ve kendi XP yolunu oluştur.</p></div></section><section className="catalogue-section" aria-labelledby="catalogue-heading"><div className="section-heading"><div><p className="kicker">Meydan okuma seç</p><h2 id="catalogue-heading">Hikâyeni seç, izini bırak.</h2></div><p>{visibleContents.length} yayın</p></div><label className="catalogue-search" htmlFor="catalogue-search">İçerik ara<input id="catalogue-search" type="search" value={searchQuery} onChange={(event) => { setSearchQuery(event.target.value); setCurrentPage(0); }} placeholder="Dizi, film veya belgesel adı" /></label><ErrorNotice error={error} />{isLoading ? <p className="loading-copy" aria-live="polite">İçerikler yükleniyor…</p> : <><ul className="viewer-grid">{page.items.map((content) => <ContentTile key={content.id} api={api} content={content} onOpen={() => openContent(content.id)} />)}</ul>{contents.length === 0 ? <p className="empty-state">Henüz yayınlanmış içerik yok.</p> : page.items.length === 0 && <p className="empty-state">Aramana uygun içerik bulunamadı.</p>}{page.totalPages > 1 && <Pagination page={page} onPageChange={setCurrentPage} />}</>}</section></>;
}

function ContentTile({ api, content, onOpen }: { api: PublicApi; content: PublicContentSummary; onOpen: () => void }) { return <li><article className="viewer-tile"><button className="tile-button" onClick={onOpen} aria-label={`${content.title} ayrıntılarını aç`}><Cover api={api} content={content} /><span className="tile-copy"><span className="content-label">{content.contentType === "SERIES" ? "Dizi" : "Film"}</span><strong>{content.title}</strong><span>{content.description || "Bu içerik için kısa açıklama yakında."}</span></span></button></article></li>; }

function QuizCatalogue({ api, onStartQuiz }: { api: PublicApi; onStartQuiz: (quizId: string) => void }) {
  const [quizzes, setQuizzes] = useState<PublishedQuizSummary[]>([]); const [contents, setContents] = useState<PublicContentSummary[]>([]); const [quizResults, setQuizResults] = useState<QuizResultSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState(""); const [quizSort, setQuizSort] = useState<QuizSort>("CONTENT_TITLE");
  useEffect(() => { let active = true; setIsLoading(true); setError(null); Promise.all([api.listPublishedQuizzes(), api.listAllContents(), loadQuizResults(api)]).then(([loadedQuizzes, loadedContents, loadedQuizResults]) => { if (active) { setQuizzes(loadedQuizzes); setContents(loadedContents); setQuizResults(loadedQuizResults); } }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api]);
  const visibleQuizzes = sortQuizDiscoveries(filterQuizDiscoveries(quizzes, contents, searchQuery), contents, quizSort);
  return <section className="detail-section quiz-discovery"><p className="kicker">Quizler</p><h1 className="story-title">Bir hikâye seç, kendini dene.</h1><p className="quiz-discovery__intro">Kullanıma açık bütün quizleri adlarıyla arayıp inceleyebilirsin.</p><ErrorNotice error={error} />{!isLoading && quizzes.length > 0 && <div className="quiz-filters"><label htmlFor="quiz-search">Quiz veya içerik ara<input id="quiz-search" type="search" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Örn. Gönül Dağı" /></label><label htmlFor="quiz-sort">Sıralama<select id="quiz-sort" value={quizSort} onChange={(event) => setQuizSort(event.target.value as QuizSort)}><option value="CONTENT_TITLE">İçerik adına göre</option><option value="QUIZ_TITLE">Quiz adına göre</option><option value="QUESTION_COUNT">Soru sayısı: çoktan aza</option></select></label><p aria-live="polite">{visibleQuizzes.length} quiz</p></div>}{isLoading ? <p className="loading-copy" aria-live="polite">Quizler yükleniyor…</p> : quizzes.length === 0 ? <p className="empty-state">Henüz kullanıma açık bir quiz yok.</p> : visibleQuizzes.length === 0 ? <p className="empty-state">Aramana uygun quiz bulunamadı.</p> : <ul className="quiz-list">{visibleQuizzes.map((quiz) => { const content = contents.find((candidate) => candidate.id === quiz.contentId); const earnedXp = quizEarnedXp(quiz.quizId, quizResults); return <li key={quiz.quizId}><article className="quiz-card">{content && <Cover api={api} content={content} />}<div className="quiz-card__copy"><p className="quiz-content-title">{quizContentTitle(quiz, contents)}</p><p className="content-label">{publicScopeLabel(quiz)} · {quiz.questionCount} soru</p><h2>{quiz.title}</h2><p>{quiz.description || "Hikâyeyi ne kadar hatırladığını görmek için quizi çöz."}</p>{earnedXp !== null && <p className="quiz-earned-xp">Kazanılan XP: {earnedXp}</p>}<button className="quiz-start" onClick={() => onStartQuiz(quiz.quizId)}>Quiz'e başla <span aria-hidden="true">→</span></button></div></article></li>; })}</ul>}</section>;
}

function ContentDetail({ api, contentId, onBack, onStartQuiz }: { api: PublicApi; contentId: string; onBack: () => void; onStartQuiz: (quizId: string) => void }) {
  const [content, setContent] = useState<PublicContent | null>(null); const [quizzes, setQuizzes] = useState<PublishedQuiz[]>([]); const [quizResults, setQuizResults] = useState<QuizResultSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { let active = true; setIsLoading(true); setError(null); Promise.allSettled([api.getContent(contentId), api.listQuizzes(contentId), loadQuizResults(api)]).then(([contentResult, quizResult, resultsResult]) => { if (!active) return; if (contentResult.status === "fulfilled") setContent(contentResult.value); else setError(asPublicError(contentResult.reason)); if (quizResult.status === "fulfilled") setQuizzes(quizResult.value); else if (contentResult.status === "fulfilled") setError(asPublicError(quizResult.reason)); if (resultsResult.status === "fulfilled") setQuizResults(resultsResult.value); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api, contentId]);
  if (isLoading) return <p className="loading-copy" aria-live="polite">İçerik hazırlanıyor…</p>; if (!content) return <section className="detail-section"><ErrorNotice error={error} /><button className="text-button" onClick={onBack}>← Keşfete dön</button></section>;
  return <><section className="detail-hero-viewer"><Cover api={api} content={content} prominent /><div><button className="text-button light" onClick={onBack}>← Keşfete dön</button><p className="kicker">{content.contentType === "SERIES" ? "Dizi" : "Film"}</p><h1>{content.title}</h1><p>{content.description || "Bu içerik için kısa açıklama yakında."}</p></div></section><section className="detail-section"><ErrorNotice error={error} /><QuizShelf content={content} quizzes={quizzes} quizResults={quizResults} onStartQuiz={onStartQuiz} /></section></>;
}

function QuizShelf({ content, quizzes, quizResults, onStartQuiz }: { content: PublicContent; quizzes: PublishedQuiz[]; quizResults: QuizResultSummary[]; onStartQuiz: (quizId: string) => void }) { return <section className="quiz-shelf" aria-labelledby="quiz-heading"><div className="section-heading"><div><p className="kicker">Etkileşim</p><h2 id="quiz-heading">Bu içerikle ilgili quizler</h2></div><p>{quizzes.length} quiz</p></div>{quizzes.length === 0 ? <p className="empty-state">Bu içerik için henüz yayınlanmış bir quiz yok.</p> : <ul className="quiz-list">{quizzes.map((quiz) => { const earnedXp = quizEarnedXp(quiz.quizId, quizResults); return <li key={quiz.quizId}><article><p className="content-label">{publicScopeLabel(quiz, content)} · {quiz.questions.length} soru</p><h3>{quiz.title}</h3><p>{quiz.description || "Hazır olduğunda hikâyeyi ne kadar hatırladığını dene."}</p>{earnedXp !== null && <p className="quiz-earned-xp">Kazanılan XP: {earnedXp}</p>}<button className="quiz-start" onClick={() => onStartQuiz(quiz.quizId)}>Quiz'e başla <span aria-hidden="true">→</span></button></article></li>; })}</ul>}</section>; }

export function quizEarnedXp(quizId: string, quizResults: QuizResultSummary[]) { return quizResults.find((result) => result.quizId === quizId)?.earnedXp ?? null; }
async function loadQuizResults(api: PublicApi): Promise<QuizResultSummary[]> { try { return await api.listQuizResults(); } catch { return []; } }
export function quizContentTitle(quiz: Pick<PublishedQuizSummary, "contentId">, contents: Array<Pick<PublicContentSummary, "id" | "title">>) { return contents.find((content) => content.id === quiz.contentId)?.title ?? "İçerik"; }
export function filterQuizDiscoveries<T extends Pick<PublishedQuizSummary, "contentId" | "title" | "description">>(quizzes: T[], contents: Array<Pick<PublicContentSummary, "id" | "title">>, searchQuery: string): T[] {
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase("tr-TR");
  return quizzes.filter((quiz) => {
    if (!normalizedQuery) return true;
    return `${quiz.title} ${quiz.description ?? ""} ${quizContentTitle(quiz, contents)}`.toLocaleLowerCase("tr-TR").includes(normalizedQuery);
  });
}
export function filterContentsBySearch<T extends Pick<PublicContentSummary, "title" | "description">>(contents: T[], searchQuery: string): T[] { const normalizedQuery = searchQuery.trim().toLocaleLowerCase("tr-TR"); return normalizedQuery ? contents.filter((content) => `${content.title} ${content.description ?? ""}`.toLocaleLowerCase("tr-TR").includes(normalizedQuery)) : contents; }
export function orderContentsByRecentViews<T extends Pick<PublicContentSummary, "id">>(contents: T[], recentContentIds: string[]): T[] { const positions = new Map(recentContentIds.map((contentId, index) => [contentId, index])); return [...contents].sort((firstContent, secondContent) => (positions.get(firstContent.id) ?? Number.MAX_SAFE_INTEGER) - (positions.get(secondContent.id) ?? Number.MAX_SAFE_INTEGER)); }
export function rememberRecentContent(recentContentIds: string[], contentId: string): string[] { return [contentId, ...recentContentIds.filter((recentId) => recentId !== contentId)].slice(0, 50); }
export function paginateContents<T>(contents: T[], requestedPage: number, size: number): Omit<PublicContentPage, "items"> & { items: T[] } { const totalPages = Math.ceil(contents.length / size); const page = Math.min(requestedPage, Math.max(totalPages - 1, 0)); return { items: contents.slice(page * size, (page + 1) * size), page, size, totalItems: contents.length, totalPages }; }
export function sortQuizDiscoveries<T extends Pick<PublishedQuizSummary, "contentId" | "title" | "questionCount">>(quizzes: T[], contents: Array<Pick<PublicContentSummary, "id" | "title">>, quizSort: QuizSort): T[] {
  const turkishCollator = new Intl.Collator("tr-TR");
  return [...quizzes].sort((firstQuiz, secondQuiz) => {
    if (quizSort === "QUESTION_COUNT") return secondQuiz.questionCount - firstQuiz.questionCount || turkishCollator.compare(firstQuiz.title, secondQuiz.title);
    if (quizSort === "QUIZ_TITLE") return turkishCollator.compare(firstQuiz.title, secondQuiz.title);
    return turkishCollator.compare(quizContentTitle(firstQuiz, contents), quizContentTitle(secondQuiz, contents)) || turkishCollator.compare(firstQuiz.title, secondQuiz.title);
  });
}
export function publicScopeLabel(quiz: Pick<PublishedQuiz, "scopeType"> & Partial<Pick<PublishedQuiz, "seasonId" | "episodeId">>, content?: Pick<PublicContent, "seasons">) {
  if (quiz.scopeType === "CONTENT") return "İçerik geneli";
  const season = content?.seasons.find((candidate) => candidate.id === quiz.seasonId);
  if (quiz.scopeType === "SEASON") return season ? `${season.seasonNumber}. Sezon` : "Sezon quizi";
  const episode = season?.episodes.find((candidate) => candidate.id === quiz.episodeId);
  return season && episode ? `${season.seasonNumber}. Sezon · ${episode.episodeNumber}. Bölüm` : "Bölüm quizi";
}

function QuizExperience({ api, quizId, onExit, onViewProfile }: { api: PublicApi; quizId: string; onExit: () => void; onViewProfile: () => void }) {
  const [quiz, setQuiz] = useState<PublishedQuiz | null>(null); const [attempt, setAttempt] = useState<QuizAttempt | null>(null); const [answerResult, setAnswerResult] = useState<AnswerSubmissionResult | null>(null); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true); const [isStarting, setIsStarting] = useState(false); const [isSubmitting, setIsSubmitting] = useState(false); const [pendingAnswer, setPendingAnswer] = useState<{ questionId: string; optionId: string; key: string } | null>(null);
  useEffect(() => { let active = true; api.getQuiz(quizId).then((loadedQuiz) => { if (active) setQuiz(loadedQuiz); }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api, quizId]);
  async function start() { setIsStarting(true); setError(null); try { setAttempt(await api.startAttempt(quizId)); } catch (reason) { setError(asPublicError(reason)); } finally { setIsStarting(false); } }
  async function submit(questionId: string, optionId: string, key = createRequestKey()) { setIsSubmitting(true); setError(null); const pending = { questionId, optionId, key }; setPendingAnswer(pending); try { setAnswerResult(await api.submitAnswer(attempt!.attemptId, questionId, optionId, key)); } catch (reason) { setError(asPublicError(reason)); } finally { setIsSubmitting(false); } }
  async function timeout(questionId: string) { if (!attempt || isSubmitting || answerResult) return; setIsSubmitting(true); setError(null); try { const updated = await api.timeoutQuestion(attempt.attemptId, questionId, `timeout-${questionId}`); const feedback = updated.submittedAnswers.at(-1)!; setAnswerResult({ attemptId: updated.attemptId, feedback, attemptStatus: updated.status, score: updated.score, earnedXp: updated.earnedXp, questionDeadline: updated.status === "ACTIVE" ? updated.questionDeadline : null, nextQuestion: updated.currentQuestion }); } catch (reason) { setError(asPublicError(reason)); } finally { setIsSubmitting(false); } }
  async function continueAfterAnswer() {
    if (!attempt || !answerResult) return;
    if (answerResult.attemptStatus === "COMPLETED") {
      setAttempt({ ...attempt, status: answerResult.attemptStatus, score: answerResult.score, earnedXp: answerResult.earnedXp, questionDeadline: null, answeredQuestionCount: attempt.answeredQuestionCount + 1, currentQuestion: null, submittedAnswers: [...attempt.submittedAnswers, answerResult.feedback] });
      setAnswerResult(null); setPendingAnswer(null); return;
    }
    setIsSubmitting(true); setError(null);
    try { setAttempt(await api.startNextQuestion(attempt.attemptId)); setAnswerResult(null); setPendingAnswer(null); }
    catch (reason) { setError(asPublicError(reason)); }
    finally { setIsSubmitting(false); }
  }
  if (isLoading) return <p className="loading-copy" aria-live="polite">Quiz hazırlanıyor…</p>; if (!quiz) return <section className="detail-section"><ErrorNotice error={error} /><button className="text-button" onClick={onExit}>← Geri dön</button></section>;
  if (!attempt) return <section className="quiz-intro"><p className="kicker">{quiz.questions.length} soru · Her soru 30 saniye</p><h1>{quiz.title}</h1><p>{quiz.description || "Bu hikâyeyi ne kadar hatırladığını gör."}</p><ErrorNotice error={error} /><div className="timing-choice"><button disabled={isStarting} onClick={start}><strong>Quiz'e başla</strong><span>Cevabını erken verip hemen ilerleyebilirsin.</span></button></div><button className="text-button" onClick={onExit}>Vazgeç</button></section>;
  if (attempt.status === "COMPLETED" && !answerResult) return <QuizResult attempt={attempt} onExit={onExit} onViewProfile={onViewProfile} />;
  const question = attempt.currentQuestion; if (!question) return <QuizResult attempt={attempt} onExit={onExit} onViewProfile={onViewProfile} />;
  const restoredAnswerResult = attempt.status === "AWAITING_NEXT_QUESTION" && !answerResult
    ? { attemptId: attempt.attemptId, feedback: attempt.submittedAnswers.at(-1)!, attemptStatus: attempt.status, score: attempt.score, earnedXp: attempt.earnedXp, questionDeadline: null, nextQuestion: question }
    : answerResult;
  return <QuizQuestion api={api} attempt={attempt} question={question} answerResult={restoredAnswerResult} error={error} isSubmitting={isSubmitting} pendingAnswer={pendingAnswer} onExit={onExit} onSubmit={submit} onTimeout={timeout} onContinue={continueAfterAnswer} />;
}

function QuizQuestion({ api, attempt, question, answerResult, error, isSubmitting, pendingAnswer, onExit, onSubmit, onTimeout, onContinue }: { api: PublicApi; attempt: QuizAttempt; question: NonNullable<QuizAttempt["currentQuestion"]>; answerResult: AnswerSubmissionResult | null; error: PublicApiRequestError | null; isSubmitting: boolean; pendingAnswer: { questionId: string; optionId: string; key: string } | null; onExit: () => void; onSubmit: (questionId: string, optionId: string, key?: string) => Promise<void>; onTimeout: (questionId: string) => Promise<void>; onContinue: () => void }) {
  const timerDeadline = questionTimerDeadline(attempt.questionDeadline, attempt.status === "ACTIVE", isSubmitting || pendingAnswer !== null || answerResult !== null);
  const remainingTime = useRemainingTime(timerDeadline);
  const timeExpired = timerDeadline !== null && remainingTime === 0;
  useEffect(() => { if (timeExpired && !answerResult && !isSubmitting) void onTimeout(question.questionId); }, [answerResult, isSubmitting, onTimeout, question.questionId, timeExpired]);
  return <section className="quiz-play">
    <header className="quiz-play__header">
      <button className="quiz-exit" onClick={onExit}>← Quizden çık</button>
      <div className="quiz-status"><span>Soru {attempt.answeredQuestionCount + 1} / {attempt.totalQuestionCount}</span><strong>{remainingTime} sn</strong></div>
      <progress value={attempt.answeredQuestionCount} max={attempt.totalQuestionCount}>İlerleme</progress>
    </header>
    <ErrorNotice error={error} retry={pendingAnswer ? () => onSubmit(pendingAnswer.questionId, pendingAnswer.optionId, pendingAnswer.key) : undefined} />
    {timeExpired && !answerResult && <p className="time-expired" role="status">Süre doldu; soru kaydediliyor…</p>}
    {answerResult ? <AnswerReveal result={answerResult} isContinuing={isSubmitting} onContinue={onContinue} /> : <>
      <div className={`question-card${question.visual ? " question-card--with-visual" : ""}`}>
        {question.visual && <AuthenticatedImage api={api} src={question.visual.contentUrl} alt={question.visual.role === "INFORMATIVE" ? question.visual.alternativeText : ""} fallback={<p className="media-fallback">Soru görseli yüklenemedi. Erişilebilir soru metnini kullanabilirsiniz.</p>} />}
        <div className="question-copy"><p className="question-kicker">Doğru cevabı seç</p><h1>{question.prompt}</h1></div>
      </div>
      <div className="answer-options" role="group" aria-label="Cevap seçenekleri">{question.options.map((option, index) => <button key={option.optionId} disabled={isSubmitting || pendingAnswer !== null || timeExpired} onClick={() => onSubmit(question.questionId, option.optionId)}><span className="option-marker" aria-hidden="true">{String.fromCharCode(65 + index)}</span><span>{option.text}</span></button>)}</div>
    </>}
  </section>;
}

function AnswerReveal({ result, isContinuing, onContinue }: { result: AnswerSubmissionResult; isContinuing: boolean; onContinue: () => void }) {
  const timedOut = result.feedback.resultStatus === "TIMED_OUT";
  const { correct, correctOptionText } = result.feedback;
  return <section className={`answer-reveal ${correct ? "correct" : "incorrect"}`} aria-live="assertive">
    <p>{correct ? "Doğru cevap!" : timedOut ? "Süre doldu" : "Bu kez değil"}</p>
    <h1>{result.feedback.awardedPoints} puan</h1>
    {(!correct || timedOut) && correctOptionText && (
      <div className="answer-reveal__correct-option">
        <span>Doğru Cevap:</span>
        <strong>{correctOptionText}</strong>
      </div>
    )}
    <button disabled={isContinuing} onClick={onContinue}>{result.attemptStatus === "COMPLETED" ? "Sonucu gör" : "Sonraki soruya geç"} <span aria-hidden="true">→</span></button>
  </section>;
}
export function quizResultXpMessage(earnedXp: number | null) { return earnedXp === null ? "XP işleniyor; profilinde kısa süre sonra görünebilir." : earnedXp === 0 ? "Bu bir alıştırma çözümüydü; ek XP kazanmadın." : `+${earnedXp} XP kazandın.`; }
export function quizResultCorrectAnswerSummary(attempt: { submittedAnswers: Array<Pick<AnswerFeedback, "correct">>; totalQuestionCount: number }) { const correctAnswerCount = attempt.submittedAnswers.filter((answer) => answer.correct).length; return `${correctAnswerCount} / ${attempt.totalQuestionCount} doğru`; }
function QuizResult({ attempt, onExit, onViewProfile }: { attempt: QuizAttempt; onExit: () => void; onViewProfile: () => void }) { return <section className="quiz-result"><p className="kicker">Quiz tamamlandı</p><h1>{attempt.score} puan</h1><p className="quiz-result__accuracy">{quizResultCorrectAnswerSummary(attempt)}</p><p>{quizResultXpMessage(attempt.earnedXp)}</p><div><button className="quiz-start" onClick={onViewProfile}>Profilime git</button><button className="text-button" onClick={onExit}>Keşfete dön</button></div></section>; }

function Profile({ api }: { api: PublicApi }) { const [data, setData] = useState<{ xp: XpSummary; leaderboard: Leaderboard } | null>(null); const [error, setError] = useState<PublicApiRequestError | null>(null); useEffect(() => { let active = true; Promise.all([api.getXp(), api.getGlobalLeaderboard()]).then(([xp, leaderboard]) => { if (active) setData({ xp, leaderboard }); }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }); return () => { active = false; }; }, [api]); return <section className="detail-section profile-page"><p className="kicker">Profil</p><h1 className="story-title">İlerleme alanın</h1><ErrorNotice error={error} />{data ? <><div className="profile-stats"><article><span>Toplam XP</span><strong>{data.xp.totalXp}</strong></article><article><span>Tamamlanan işlem</span><strong>{data.xp.transactionCount}</strong></article><article><span>Global sıra</span><strong>{data.leaderboard.currentUser ? `#${data.leaderboard.currentUser.position}` : "—"}</strong></article></div><p className="profile-note">XP toplamı mesajlaşma consumer'ı çalışana kadar kısa süreli eski kalabilir.</p><section className="profile-ranking" aria-labelledby="profile-ranking-heading"><div className="section-heading"><div><p className="kicker">Tüm zamanlar</p><h2 id="profile-ranking-heading">Genel sıralama</h2></div><p>{data.leaderboard.participantCount} katılımcı</p></div><p className="leaderboard-meta">{data.leaderboard.dataSource === "REDIS" ? "Güncellenen görünüm" : "Doğrulanmış kaynak"}</p><ol className="leaderboard-list">{data.leaderboard.leaders.map((entry) => <li className={entry.currentUser ? "current-user" : ""} key={entry.userId}><span>#{entry.position}</span><strong className="leaderboard-name">{leaderboardEntryName(entry)}</strong><strong>{entry.totalXp} XP</strong></li>)}</ol>{data.leaderboard.currentUser && !data.leaderboard.leaders.some((entry) => entry.currentUser) && <p className="own-rank">Senin sıran: #{data.leaderboard.currentUser.position} · {data.leaderboard.currentUser.totalXp} XP</p>}</section></> : <p className="loading-copy">Profilin hazırlanıyor…</p>}</section>; }
function Cover({ api, content, prominent = false }: { api: PublicApi; content: PublicContentSummary; prominent?: boolean }) { const className = `viewer-cover ${prominent ? "viewer-cover--prominent" : ""} viewer-cover--${content.contentType.toLowerCase()}`; return <div className={className}>{content.coverImageUrl ? <AuthenticatedImage api={api} src={content.coverImageUrl} alt={content.coverAlternativeText || ""} fallback={<span aria-hidden="true">{content.title.slice(0, 1)}</span>} /> : <span aria-hidden="true">{content.title.slice(0, 1)}</span>}</div>; }
function AuthenticatedImage({ api, src, alt, fallback }: { api: PublicApi; src: string; alt: string; fallback: ReactNode }) { const [objectUrl, setObjectUrl] = useState<string | null>(null); const [failed, setFailed] = useState(false); useEffect(() => { let active = true; let createdUrl: string | null = null; setObjectUrl(null); setFailed(false); api.getMedia(src).then((blob) => { if (!active) return; createdUrl = URL.createObjectURL(blob); setObjectUrl(createdUrl); }).catch(() => { if (active) setFailed(true); }); return () => { active = false; if (createdUrl) URL.revokeObjectURL(createdUrl); }; }, [api, src]); if (failed) return fallback; return objectUrl ? <img src={objectUrl} alt={alt} /> : <span className="media-loading" aria-hidden="true" />; }
function Pagination({ page, onPageChange }: { page: PublicContentPage; onPageChange: (page: number) => void }) { return <nav className="viewer-pagination" aria-label="İçerik sayfaları"><button disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)}>Önceki</button><span>{page.page + 1} / {Math.max(page.totalPages, 1)}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>Sonraki</button></nav>; }
function ErrorNotice({ error, retry }: { error: PublicApiRequestError | null; retry?: () => void }) { if (!error) return null; return <section className="viewer-error" role="alert"><h2>Şu an yüklenemedi</h2><p>{error.message}</p><p>Hata kodu: <code>{error.code}</code> · İz kimliği: <code>{error.traceId}</code></p>{retry && <button onClick={retry}>Tekrar dene</button>}</section>; }
export function resolveViewerView(view: string | null): View { return view === "quizzes" ? "quizzes" : view === "profile" || view === "leaderboard" ? "profile" : "home"; }
function readRoute(): ViewerRoute { const query = new URLSearchParams(window.location.search); return { contentId: query.get("content"), quizId: query.get("quiz"), view: resolveViewerView(query.get("view")) }; }
function readRecentContentIds(): string[] { try { const savedIds = JSON.parse(window.localStorage.getItem(RECENT_CONTENT_IDS_KEY) ?? "[]"); return Array.isArray(savedIds) ? savedIds.filter((contentId): contentId is string => typeof contentId === "string") : []; } catch { return []; } }
function saveRecentContentIds(contentIds: string[]) { try { window.localStorage.setItem(RECENT_CONTENT_IDS_KEY, JSON.stringify(contentIds)); } catch { /* Browsing still works when local storage is unavailable. */ } }
function setRouteParam(url: URL, key: string, value: string | null) { if (value) url.searchParams.set(key, value); else url.searchParams.delete(key); }
function createRequestKey(): string { return globalThis.crypto?.randomUUID?.() ?? `answer-${Date.now()}-${Math.random().toString(36).slice(2)}`; }
export function questionTimerDeadline(questionDeadline: string | null, attemptIsActive: boolean, answerIsBeingResolved: boolean): string | null {
  return attemptIsActive && !answerIsBeingResolved ? questionDeadline : null;
}
function useRemainingTime(deadline: string | null) { const [now, setNow] = useState(Date.now); useEffect(() => { if (!deadline) return; setNow(Date.now()); const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, [deadline]); return deadline ? Math.max(0, Math.ceil((new Date(deadline).getTime() - now) / 1000)) : 0; }
function formatRemainingTime(seconds: number) { return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`; }
export function leaderboardEntryName(entry: LeaderboardEntry) {
  return entry.displayName?.trim() || (entry.currentUser ? "Sen" : "Kullanıcı");
}
function asPublicError(reason: unknown): PublicApiRequestError { return reason instanceof PublicApiRequestError ? reason : new PublicApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: "Beklenmeyen bir istemci hatası oluştu.", traceId: "unavailable", status: 0 }); }
