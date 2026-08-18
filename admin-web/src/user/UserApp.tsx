import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { AuthApi, type AccountSession } from "./auth-api";
import { PublicApi, PublicApiRequestError } from "./public-api";
import type { AnswerFeedback, AnswerSubmissionResult, AttemptQuestion, Leaderboard, LeaderboardEntry, PublicContent, PublicContentPage, PublicContentSummary, PublishedQuiz, PublishedQuizSummary, QuizAttempt, QuizResultSummary, XpSummary } from "./types";
import tabiiLogoUrl from "../../../gorseller/Tabii_-_TRT_Logo.png";
import platformArtworkUrl from "../../../gorseller/tabii-dijital-platform-2041030.jpg";
import { readStoredLanguage, translate, useI18n } from "../i18n/I18nContext";
import { LanguageSwitcher } from "../i18n/LanguageSwitcher";
import type { Language } from "../i18n/types";

const PAGE_SIZE = 12;
const RECENT_CONTENT_IDS_KEY = "viewer-recent-content-ids";
type View = "home" | "quizzes" | "profile";
type QuizSort = "CONTENT_TITLE" | "QUIZ_TITLE" | "QUESTION_COUNT";
interface ViewerRoute { contentId: string | null; quizId: string | null; view: View; }

export function UserApp() {
  const { t } = useI18n();
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

  if (account === undefined) return <main className="viewer-shell sign-in-shell"><LanguageSwitcher /><p className="loading-copy">{t("viewer.sessionChecking")}</p></main>;
  if (!account) return <AccountAccess authApi={authApi} onSignedIn={setAccount} />;
  return <main className="viewer-shell">
    <SiteHeader account={account} view={route.view} onNavigate={(view) => navigate({ view, contentId: null, quizId: null })} onSignOut={async () => { try { await authApi.logout(); } finally { setAccount(null); } }} />
    {route.quizId ? <QuizExperience api={api} quizId={route.quizId} onExit={() => navigate({ quizId: null })} onViewProfile={() => navigate({ quizId: null, contentId: null, view: "profile" })} /> : route.contentId
      ? <ContentDetail api={api} contentId={route.contentId} onBack={() => navigate({ contentId: null })} onStartQuiz={(quizId) => navigate({ quizId })} />
      : route.view === "quizzes" ? <QuizCatalogue api={api} onStartQuiz={(quizId) => navigate({ quizId })} /> : route.view === "profile" ? <Profile api={api} /> : <Catalogue api={api} onOpen={(contentId) => navigate({ contentId })} />}
  </main>;
}

function SiteHeader({ account, view, onNavigate, onSignOut }: { account: AccountSession; view: View; onNavigate: (view: View) => void; onSignOut: () => Promise<void> }) {
  const { t } = useI18n();
  return <header className="viewer-header"><a className="viewer-brand" href="/" onClick={(event) => { event.preventDefault(); onNavigate("home"); }} aria-label={t("viewer.homeAria")}><img src={tabiiLogoUrl} alt="tabii" /><span><strong>Hikâye</strong> İzi</span><em>{t("viewer.tagline")}</em></a>
    <nav aria-label={t("viewer.menu")}><button className={view === "home" ? "active" : ""} onClick={() => onNavigate("home")}>{t("viewer.discover")}</button><button className={view === "quizzes" ? "active" : ""} onClick={() => onNavigate("quizzes")}>{t("viewer.quizzes")}</button><button className={view === "profile" ? "active" : ""} onClick={() => onNavigate("profile")}>{t("viewer.profile")}</button></nav><LanguageSwitcher /><p><span>{account.displayName}</span><button className="sign-out" onClick={() => void onSignOut()}>{t("viewer.signOut")}</button></p></header>;
}

function AccountAccess({ authApi, onSignedIn }: { authApi: AuthApi; onSignedIn: (account: AccountSession) => void }) {
  const { t } = useI18n();
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
        setSuccessMessage(t("viewer.resetRequested"));
      } else if (mode === "reset" && resetToken) {
        await authApi.resetPassword(resetToken, password);
        window.history.replaceState({}, "", "/");
        setMode("login"); setPassword("");
        setSuccessMessage(t("viewer.resetSucceeded"));
      } else {
        onSignedIn(mode === "login" ? await authApi.login(email, password) : await authApi.register(email, displayName, password));
      }
    }
    catch (reason) { setError(asPublicError(reason)); }
    finally { setIsSubmitting(false); }
  }
  function changeMode(nextMode: AccountMode) { setMode(nextMode); setError(null); setSuccessMessage(null); }
  const isLogin = mode === "login";
  const title = t(mode === "login" ? "viewer.loginTitle" : mode === "register" ? "viewer.registerTitle" : mode === "forgot" ? "viewer.forgotTitle" : "viewer.resetTitle");
  const description = t(mode === "login" ? "viewer.loginDescription" : mode === "register" ? "viewer.registerDescription" : mode === "forgot" ? "viewer.forgotDescription" : "viewer.resetDescription");
  return <main className="viewer-shell sign-in-shell">
    <div className="sign-in-backdrop" aria-hidden="true"><img src={platformArtworkUrl} alt="" /></div>
    <header className="sign-in-header">
      <a className="sign-in-brand" href="/" aria-label={t("viewer.homeAria")}><img className="sign-in-wordmark" src={platformArtworkUrl} alt="tabii" /><span>Hikâye İzi</span></a>
      <LanguageSwitcher />
      <button className="auth-switch auth-switch--header" type="button" onClick={() => changeMode(isLogin ? "register" : "login")}>{isLogin ? t("viewer.createAccount") : t("admin.signIn")}</button>
    </header>
    <section className="local-access" aria-labelledby="account-access-title">
      <p className="kicker">Hikâye İzi</p>
      <h1 id="account-access-title">{title}</h1>
      <p>{description}</p>
      <form onSubmit={submit}>
        {mode === "register" && <label htmlFor="display-name">{t("viewer.displayName")}<input id="display-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} autoComplete="name" minLength={2} maxLength={80} placeholder={t("viewer.displayNamePlaceholder")} required /></label>}
        {mode !== "reset" && <label htmlFor="account-email">{t("common.email")}<input id="account-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" placeholder="ornek@email.com" required /></label>}
        {(mode === "login" || mode === "register" || mode === "reset") && <label htmlFor="account-password">{mode === "reset" ? t("viewer.newPassword") : t("common.password")}<input id="account-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={8} maxLength={64} placeholder={t("viewer.passwordPlaceholder")} required /></label>}
        {isLogin && <button className="forgot-password" type="button" onClick={() => changeMode("forgot")}>{t("viewer.forgotPassword")}</button>}
        <button className="auth-submit" disabled={isSubmitting || (mode === "reset" && !resetToken)}>{isSubmitting ? t("viewer.pleaseWait") : mode === "login" ? t("admin.signIn") : mode === "register" ? t("viewer.createAccount") : mode === "forgot" ? t("viewer.sendLink") : t("viewer.resetPassword")}</button>
      </form>
      <ErrorNotice error={error} />
      {successMessage && <p className="auth-success" role="status">{successMessage}</p>}
      <p className="auth-alternative">{isLogin ? t("viewer.noAccount") : t("viewer.returnToLoginQuestion")} <button className="auth-switch" type="button" onClick={() => changeMode(isLogin ? "register" : "login")}>{isLogin ? t("viewer.createFreeAccount") : t("admin.signIn")}</button></p>
      <p className="sign-in-note">{t("viewer.localAccountNotice")}</p>
    </section>
  </main>;
}

function Catalogue({ api, onOpen }: { api: PublicApi; onOpen: (contentId: string) => void }) {
  const { language, t } = useI18n();
  const [contents, setContents] = useState<PublicContentSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true); const [currentPage, setCurrentPage] = useState(0); const [searchQuery, setSearchQuery] = useState(""); const [recentContentIds, setRecentContentIds] = useState(readRecentContentIds);
  useEffect(() => { let active = true; setIsLoading(true); setError(null); api.listAllContents().then((result) => { if (active) setContents(result); }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api]);
  const visibleContents = orderContentsByRecentViews(filterContentsBySearch(contents, searchQuery, language), recentContentIds);
  const page = paginateContents(visibleContents, currentPage, PAGE_SIZE);
  function openContent(contentId: string) { const nextIds = rememberRecentContent(recentContentIds, contentId); setRecentContentIds(nextIds); saveRecentContentIds(nextIds); onOpen(contentId); }
  return <><section className="viewer-hero"><img className="hero-artwork" src={platformArtworkUrl} alt="" /><div className="hero-copy"><p>Hikâye İzi</p><h1>{t("viewer.heroTitle")}</h1><p className="hero-description">{t("viewer.heroDescription")}</p></div></section><section className="catalogue-section" aria-labelledby="catalogue-heading"><div className="section-heading"><div><p className="kicker">{t("viewer.chooseChallenge")}</p><h2 id="catalogue-heading">{t("viewer.chooseStory")}</h2></div><p>{t("viewer.publications", { count: visibleContents.length })}</p></div><label className="catalogue-search" htmlFor="catalogue-search">{t("viewer.searchContent")}<input id="catalogue-search" type="search" value={searchQuery} onChange={(event) => { setSearchQuery(event.target.value); setCurrentPage(0); }} placeholder={t("viewer.searchContentPlaceholder")} /></label><ErrorNotice error={error} />{isLoading ? <p className="loading-copy" aria-live="polite">{t("viewer.contentsLoading")}</p> : <><ul className="viewer-grid">{page.items.map((content) => <ContentTile key={content.id} api={api} content={content} onOpen={() => openContent(content.id)} />)}</ul>{contents.length === 0 ? <p className="empty-state">{t("viewer.noPublishedContent")}</p> : page.items.length === 0 && <p className="empty-state">{t("viewer.noContentResults")}</p>}{page.totalPages > 1 && <Pagination page={page} onPageChange={setCurrentPage} />}</>}</section></>;
}

function ContentTile({ api, content, onOpen }: { api: PublicApi; content: PublicContentSummary; onOpen: () => void }) { const { t } = useI18n(); return <li><article className="viewer-tile"><button className="tile-button" onClick={onOpen} aria-label={t("viewer.openDetailsAria", { title: content.title })}><Cover api={api} content={content} /><span className="tile-copy"><span className="content-label">{t(content.contentType === "SERIES" ? "common.series" : "common.film")}</span><strong>{content.title}</strong><span>{content.description || t("viewer.contentDescriptionSoon")}</span></span></button></article></li>; }

function QuizCatalogue({ api, onStartQuiz }: { api: PublicApi; onStartQuiz: (quizId: string) => void }) {
  const { language, t } = useI18n();
  const [quizzes, setQuizzes] = useState<PublishedQuizSummary[]>([]); const [contents, setContents] = useState<PublicContentSummary[]>([]); const [quizResults, setQuizResults] = useState<QuizResultSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState(""); const [quizSort, setQuizSort] = useState<QuizSort>("CONTENT_TITLE");
  useEffect(() => { let active = true; setIsLoading(true); setError(null); Promise.all([api.listPublishedQuizzes(), api.listAllContents(), loadQuizResults(api)]).then(([loadedQuizzes, loadedContents, loadedQuizResults]) => { if (active) { setQuizzes(loadedQuizzes); setContents(loadedContents); setQuizResults(loadedQuizResults); } }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api]);
  const visibleQuizzes = sortQuizDiscoveries(filterQuizDiscoveries(quizzes, contents, searchQuery, language), contents, quizSort, language);
  return <section className="detail-section quiz-discovery"><p className="kicker">{t("viewer.quizzes")}</p><h1 className="story-title">{t("viewer.quizDiscoveryTitle")}</h1><p className="quiz-discovery__intro">{t("viewer.quizDiscoveryIntro")}</p><ErrorNotice error={error} />{!isLoading && quizzes.length > 0 && <div className="quiz-filters"><label htmlFor="quiz-search">{t("viewer.searchQuiz")}<input id="quiz-search" type="search" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder={t("viewer.searchQuizPlaceholder")} /></label><label htmlFor="quiz-sort">{t("viewer.sort")}<select id="quiz-sort" value={quizSort} onChange={(event) => setQuizSort(event.target.value as QuizSort)}><option value="CONTENT_TITLE">{t("viewer.sortByContent")}</option><option value="QUIZ_TITLE">{t("viewer.sortByQuiz")}</option><option value="QUESTION_COUNT">{t("viewer.sortByQuestionCount")}</option></select></label><p aria-live="polite">{t("viewer.quizCount", { count: visibleQuizzes.length })}</p></div>}{isLoading ? <p className="loading-copy" aria-live="polite">{t("viewer.quizzesLoading")}</p> : quizzes.length === 0 ? <p className="empty-state">{t("viewer.noPublishedQuiz")}</p> : visibleQuizzes.length === 0 ? <p className="empty-state">{t("viewer.noQuizResults")}</p> : <ul className="quiz-list">{visibleQuizzes.map((quiz) => { const content = contents.find((candidate) => candidate.id === quiz.contentId); const earnedXp = quizEarnedXp(quiz.quizId, quizResults); return <li key={quiz.quizId}><article className="quiz-card">{content && <Cover api={api} content={content} />}<div className="quiz-card__copy"><p className="quiz-content-title">{quizContentTitle(quiz, contents, language)}</p><p className="content-label">{publicScopeLabel(quiz, undefined, language)} · {t("common.questions", { count: quiz.questionCount })}</p><h2>{quiz.title}</h2><p>{quiz.description || t("viewer.quizFallback")}</p>{earnedXp !== null && <p className="quiz-earned-xp">{t("viewer.earnedXp", { xp: earnedXp })}</p>}<button className="quiz-start" onClick={() => onStartQuiz(quiz.quizId)}>{t("viewer.startQuiz")} <span aria-hidden="true">→</span></button></div></article></li>; })}</ul>}</section>;
}

function ContentDetail({ api, contentId, onBack, onStartQuiz }: { api: PublicApi; contentId: string; onBack: () => void; onStartQuiz: (quizId: string) => void }) {
  const { language, t } = useI18n();
  const [content, setContent] = useState<PublicContent | null>(null); const [quizzes, setQuizzes] = useState<PublishedQuiz[]>([]); const [quizResults, setQuizResults] = useState<QuizResultSummary[]>([]); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { let active = true; setIsLoading(true); setError(null); Promise.allSettled([api.getContent(contentId), api.listQuizzes(contentId), loadQuizResults(api)]).then(([contentResult, quizResult, resultsResult]) => { if (!active) return; if (contentResult.status === "fulfilled") setContent(contentResult.value); else setError(asPublicError(contentResult.reason)); if (quizResult.status === "fulfilled") setQuizzes(quizResult.value); else if (contentResult.status === "fulfilled") setError(asPublicError(quizResult.reason)); if (resultsResult.status === "fulfilled") setQuizResults(resultsResult.value); }).finally(() => { if (active) setIsLoading(false); }); return () => { active = false; }; }, [api, contentId]);
  if (isLoading) return <p className="loading-copy" aria-live="polite">{t("viewer.contentPreparing")}</p>; if (!content) return <section className="detail-section"><ErrorNotice error={error} /><button className="text-button" onClick={onBack}>← {t("viewer.backToDiscover")}</button></section>;
  return <><section className="detail-hero-viewer"><Cover api={api} content={content} prominent /><div><button className="text-button light" onClick={onBack}>← {t("viewer.backToDiscover")}</button><p className="kicker">{t(content.contentType === "SERIES" ? "common.series" : "common.film")}</p><h1>{content.title}</h1><p>{content.description || t("viewer.contentDescriptionSoon")}</p></div></section><section className="detail-section"><ErrorNotice error={error} /><QuizShelf content={content} quizzes={quizzes} quizResults={quizResults} language={language} onStartQuiz={onStartQuiz} /></section></>;
}

function QuizShelf({ content, quizzes, quizResults, language, onStartQuiz }: { content: PublicContent; quizzes: PublishedQuiz[]; quizResults: QuizResultSummary[]; language: Language; onStartQuiz: (quizId: string) => void }) { const { t } = useI18n(); return <section className="quiz-shelf" aria-labelledby="quiz-heading"><div className="section-heading"><div><p className="kicker">{t("viewer.interaction")}</p><h2 id="quiz-heading">{t("viewer.relatedQuizzes")}</h2></div><p>{t("viewer.quizCount", { count: quizzes.length })}</p></div>{quizzes.length === 0 ? <p className="empty-state">{t("viewer.noRelatedQuiz")}</p> : <ul className="quiz-list">{quizzes.map((quiz) => { const earnedXp = quizEarnedXp(quiz.quizId, quizResults); return <li key={quiz.quizId}><article><p className="content-label">{publicScopeLabel(quiz, content, language)} · {t("common.questions", { count: quiz.questions.length })}</p><h3>{quiz.title}</h3><p>{quiz.description || t("viewer.readyQuizFallback")}</p>{earnedXp !== null && <p className="quiz-earned-xp">{t("viewer.earnedXp", { xp: earnedXp })}</p>}<button className="quiz-start" onClick={() => onStartQuiz(quiz.quizId)}>{t("viewer.startQuiz")} <span aria-hidden="true">→</span></button></article></li>; })}</ul>}</section>; }

export function quizEarnedXp(quizId: string, quizResults: QuizResultSummary[]) { return quizResults.find((result) => result.quizId === quizId)?.earnedXp ?? null; }
async function loadQuizResults(api: PublicApi): Promise<QuizResultSummary[]> { try { return await api.listQuizResults(); } catch { return []; } }
export function quizContentTitle(quiz: Pick<PublishedQuizSummary, "contentId">, contents: Array<Pick<PublicContentSummary, "id" | "title">>, language: Language = "tr") { return contents.find((content) => content.id === quiz.contentId)?.title ?? translate(language, "viewer.contentFallback"); }
export function filterQuizDiscoveries<T extends Pick<PublishedQuizSummary, "contentId" | "title" | "description">>(quizzes: T[], contents: Array<Pick<PublicContentSummary, "id" | "title">>, searchQuery: string, language: Language = "tr"): T[] {
  const locale = language === "tr" ? "tr-TR" : "en-US";
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase(locale);
  return quizzes.filter((quiz) => {
    if (!normalizedQuery) return true;
    return `${quiz.title} ${quiz.description ?? ""} ${quizContentTitle(quiz, contents, language)}`.toLocaleLowerCase(locale).includes(normalizedQuery);
  });
}
export function filterContentsBySearch<T extends Pick<PublicContentSummary, "title" | "description">>(contents: T[], searchQuery: string, language: Language = "tr"): T[] { const locale = language === "tr" ? "tr-TR" : "en-US"; const normalizedQuery = searchQuery.trim().toLocaleLowerCase(locale); return normalizedQuery ? contents.filter((content) => `${content.title} ${content.description ?? ""}`.toLocaleLowerCase(locale).includes(normalizedQuery)) : contents; }
export function orderContentsByRecentViews<T extends Pick<PublicContentSummary, "id">>(contents: T[], recentContentIds: string[]): T[] { const positions = new Map(recentContentIds.map((contentId, index) => [contentId, index])); return [...contents].sort((firstContent, secondContent) => (positions.get(firstContent.id) ?? Number.MAX_SAFE_INTEGER) - (positions.get(secondContent.id) ?? Number.MAX_SAFE_INTEGER)); }
export function rememberRecentContent(recentContentIds: string[], contentId: string): string[] { return [contentId, ...recentContentIds.filter((recentId) => recentId !== contentId)].slice(0, 50); }
export function paginateContents<T>(contents: T[], requestedPage: number, size: number): Omit<PublicContentPage, "items"> & { items: T[] } { const totalPages = Math.ceil(contents.length / size); const page = Math.min(requestedPage, Math.max(totalPages - 1, 0)); return { items: contents.slice(page * size, (page + 1) * size), page, size, totalItems: contents.length, totalPages }; }
export function sortQuizDiscoveries<T extends Pick<PublishedQuizSummary, "contentId" | "title" | "questionCount">>(quizzes: T[], contents: Array<Pick<PublicContentSummary, "id" | "title">>, quizSort: QuizSort, language: Language = "tr"): T[] {
  const collator = new Intl.Collator(language === "tr" ? "tr-TR" : "en-US");
  return [...quizzes].sort((firstQuiz, secondQuiz) => {
    if (quizSort === "QUESTION_COUNT") return secondQuiz.questionCount - firstQuiz.questionCount || collator.compare(firstQuiz.title, secondQuiz.title);
    if (quizSort === "QUIZ_TITLE") return collator.compare(firstQuiz.title, secondQuiz.title);
    return collator.compare(quizContentTitle(firstQuiz, contents, language), quizContentTitle(secondQuiz, contents, language)) || collator.compare(firstQuiz.title, secondQuiz.title);
  });
}
export function publicScopeLabel(quiz: Pick<PublishedQuiz, "scopeType"> & Partial<Pick<PublishedQuiz, "seasonId" | "episodeId">>, content?: Pick<PublicContent, "seasons">, language: Language = "tr") {
  if (quiz.scopeType === "CONTENT") return translate(language, "viewer.generalContent");
  const season = content?.seasons.find((candidate) => candidate.id === quiz.seasonId);
  if (quiz.scopeType === "SEASON") return season ? translate(language, "viewer.season", { number: season.seasonNumber }) : translate(language, "viewer.seasonQuiz");
  const episode = season?.episodes.find((candidate) => candidate.id === quiz.episodeId);
  return season && episode ? translate(language, "viewer.episode", { season: season.seasonNumber, episode: episode.episodeNumber }) : translate(language, "viewer.episodeQuiz");
}

function QuizExperience({ api, quizId, onExit, onViewProfile }: { api: PublicApi; quizId: string; onExit: () => void; onViewProfile: () => void }) {
  const { language, t } = useI18n();
  const [quiz, setQuiz] = useState<PublishedQuiz | null>(null); const [attempt, setAttempt] = useState<QuizAttempt | null>(null); const [answerResult, setAnswerResult] = useState<AnswerSubmissionResult | null>(null); const [error, setError] = useState<PublicApiRequestError | null>(null); const [isLoading, setIsLoading] = useState(true); const [isStarting, setIsStarting] = useState(false); const [isSubmitting, setIsSubmitting] = useState(false); const [pendingAnswer, setPendingAnswer] = useState<{ questionId: string; optionId: string; key: string } | null>(null);
  useEffect(() => {
    let active = true;
    api.getQuiz(quizId).then((loadedQuiz) => {
      if (active) setQuiz(loadedQuiz);
    }).catch((reason: unknown) => {
      if (active) setError(asPublicError(reason));
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [api, quizId]);
  async function start() {
    setIsStarting(true);
    setError(null);
    try {
      const newAttempt = await api.startAttempt(quizId);
      if (newAttempt.currentQuestion?.visual?.contentUrl) void api.getMedia(newAttempt.currentQuestion.visual.contentUrl);
      setAttempt(newAttempt);
    } catch (reason) {
      setError(asPublicError(reason));
    } finally {
      setIsStarting(false);
    }
  }
  async function submit(questionId: string, optionId: string, key = createRequestKey()) {
    setIsSubmitting(true);
    setError(null);
    const pending = { questionId, optionId, key };
    setPendingAnswer(pending);
    try {
      const result = await api.submitAnswer(attempt!.attemptId, questionId, optionId, key);
      if (result.nextQuestion?.visual?.contentUrl) void api.getMedia(result.nextQuestion.visual.contentUrl);
      setAnswerResult(result);
    } catch (reason) {
      setError(asPublicError(reason));
    } finally {
      setIsSubmitting(false);
    }
  }
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
  async function exitQuiz() {
    if (attempt && attempt.status !== "COMPLETED") {
      try {
        await api.abandonAttempt(attempt.attemptId);
      } catch {
        // allow navigation even if network error occurs
      }
    }
    onExit();
  }
  if (isLoading) return <p className="loading-copy" aria-live="polite">{t("viewer.quizPreparing")}</p>; if (!quiz) return <section className="detail-section"><ErrorNotice error={error} /><button className="text-button" onClick={onExit}>← {t("viewer.back")}</button></section>;
  if (!attempt) return <section className="quiz-intro"><p className="kicker">{t("viewer.quizIntroMeta", { count: quiz.questions.length })}</p><h1>{quiz.title}</h1><p>{quiz.description || t("viewer.quizIntroFallback")}</p><ErrorNotice error={error} /><div className="timing-choice"><button disabled={isStarting} onClick={start}><strong>{t("viewer.startQuiz")}</strong><span>{t("viewer.startEarlyHint")}</span></button></div><button className="text-button" onClick={onExit}>{t("common.cancel")}</button></section>;
  if (attempt.status === "COMPLETED" && !answerResult) return <QuizResult attempt={attempt} language={language} onExit={onExit} onViewProfile={onViewProfile} />;
  const question = attempt.currentQuestion; if (!question) return <QuizResult attempt={attempt} language={language} onExit={onExit} onViewProfile={onViewProfile} />;
  const restoredAnswerResult = attempt.status === "AWAITING_NEXT_QUESTION" && !answerResult
    ? { attemptId: attempt.attemptId, feedback: attempt.submittedAnswers.at(-1)!, attemptStatus: attempt.status, score: attempt.score, earnedXp: attempt.earnedXp, questionDeadline: null, nextQuestion: question }
    : answerResult;
  return <QuizQuestion api={api} attempt={attempt} question={question} answerResult={restoredAnswerResult} error={error} isSubmitting={isSubmitting} pendingAnswer={pendingAnswer} onExit={exitQuiz} onSubmit={submit} onTimeout={timeout} onContinue={continueAfterAnswer} />;
}

function QuizQuestion({ api, attempt, question, answerResult, error, isSubmitting, pendingAnswer, onExit, onSubmit, onTimeout, onContinue }: { api: PublicApi; attempt: QuizAttempt; question: NonNullable<QuizAttempt["currentQuestion"]>; answerResult: AnswerSubmissionResult | null; error: PublicApiRequestError | null; isSubmitting: boolean; pendingAnswer: { questionId: string; optionId: string; key: string } | null; onExit: () => void; onSubmit: (questionId: string, optionId: string, key?: string) => Promise<void>; onTimeout: (questionId: string) => Promise<void>; onContinue: () => void }) {
  const { t } = useI18n();
  const timerDeadline = questionTimerDeadline(attempt.questionDeadline, attempt.status === "ACTIVE", isSubmitting || pendingAnswer !== null || answerResult !== null);
  const remainingTime = useRemainingTime(timerDeadline);
  const timeExpired = timerDeadline !== null && remainingTime === 0;
  useEffect(() => { if (timeExpired && !answerResult && !isSubmitting) void onTimeout(question.questionId); }, [answerResult, isSubmitting, onTimeout, question.questionId, timeExpired]);
  return <section className="quiz-play">
    <header className="quiz-play__header">
      <button className="quiz-exit" onClick={onExit}>← {t("viewer.exitQuiz")}</button>
      <div className="quiz-status"><span>{t("viewer.questionProgress", { current: attempt.answeredQuestionCount + 1, total: attempt.totalQuestionCount })}</span><strong>{t("common.secondsShort", { count: remainingTime })}</strong></div>
      <progress value={attempt.answeredQuestionCount} max={attempt.totalQuestionCount}>{t("viewer.progress")}</progress>
    </header>
    <ErrorNotice error={error} retry={pendingAnswer ? () => onSubmit(pendingAnswer.questionId, pendingAnswer.optionId, pendingAnswer.key) : undefined} />
    {timeExpired && !answerResult && <p className="time-expired" role="status">{t("viewer.timeExpiredSaving")}</p>}
    {answerResult ? <AnswerReveal result={answerResult} question={question} isContinuing={isSubmitting} onContinue={onContinue} /> : <>
      <div className={`question-card${question.visual ? " question-card--with-visual" : ""}`}>
        {question.visual && <AuthenticatedImage api={api} src={question.visual.contentUrl} alt={question.visual.role === "INFORMATIVE" ? question.visual.alternativeText : ""} fallback={<p className="media-fallback">{t("viewer.imageFailed")}</p>} />}
        <div className="question-copy"><p className="question-kicker">{t("viewer.chooseCorrect")}</p><h1>{question.prompt}</h1></div>
      </div>
      <div className="answer-options" role="group" aria-label={t("viewer.answerOptions")}>{question.options.map((option, index) => <button key={option.optionId} disabled={isSubmitting || pendingAnswer !== null || timeExpired} onClick={() => onSubmit(question.questionId, option.optionId)}><span className="option-marker" aria-hidden="true">{String.fromCharCode(65 + index)}</span><span>{option.text}</span></button>)}</div>
    </>}
  </section>;
}

export interface AnswerRevealDetails {
  kind: "ALTERNATIVE_TEXT" | "CORRECT_OPTION" | "NONE";
  text: string | null;
}

export function resolveAnswerRevealDetails(
  feedback: Pick<AnswerFeedback, "correct" | "resultStatus" | "correctOptionText">,
  visual?: { alternativeText?: string | null } | null
): AnswerRevealDetails {
  const visualAltText = visual?.alternativeText?.trim() || null;
  if (visualAltText) {
    return { kind: "ALTERNATIVE_TEXT", text: visualAltText };
  }
  const timedOut = feedback.resultStatus === "TIMED_OUT";
  if ((!feedback.correct || timedOut) && feedback.correctOptionText) {
    return { kind: "CORRECT_OPTION", text: feedback.correctOptionText };
  }
  return { kind: "NONE", text: null };
}

function AnswerReveal({ result, question, isContinuing, onContinue }: { result: AnswerSubmissionResult; question: AttemptQuestion; isContinuing: boolean; onContinue: () => void }) {
  const { t } = useI18n();
  const timedOut = result.feedback.resultStatus === "TIMED_OUT";
  const { correct } = result.feedback;
  const revealDetails = resolveAnswerRevealDetails(result.feedback, question.visual);

  return <section className={`answer-reveal ${correct ? "correct" : "incorrect"}`} aria-live="assertive">
    <p>{t(correct ? "viewer.correctAnswer" : timedOut ? "viewer.timeExpired" : "viewer.notThisTime")}</p>
    <h1>{t("common.points", { count: result.feedback.awardedPoints })}</h1>
    {revealDetails.kind === "ALTERNATIVE_TEXT" && (
      <div className="answer-reveal__correct-option">
        <strong>{revealDetails.text}</strong>
      </div>
    )}
    {revealDetails.kind === "CORRECT_OPTION" && (
      <div className="answer-reveal__correct-option">
        <span>{t("viewer.correctOption")}</span>
        <strong>{revealDetails.text}</strong>
      </div>
    )}
    <button disabled={isContinuing} onClick={onContinue}>{t(result.attemptStatus === "COMPLETED" ? "viewer.showResult" : "viewer.nextQuestion")} <span aria-hidden="true">→</span></button>
  </section>;
}
export function quizResultXpMessage(earnedXp: number | null, language: Language = "tr") { return earnedXp === null ? translate(language, "viewer.xpPending") : earnedXp === 0 ? translate(language, "viewer.xpPractice") : translate(language, "viewer.xpEarned", { xp: earnedXp }); }
export function quizResultCorrectAnswerSummary(attempt: { submittedAnswers: Array<Pick<AnswerFeedback, "correct">>; totalQuestionCount: number }, language: Language = "tr") { const correctAnswerCount = attempt.submittedAnswers.filter((answer) => answer.correct).length; return translate(language, "viewer.correctSummary", { correct: correctAnswerCount, total: attempt.totalQuestionCount }); }
function QuizResult({ attempt, language, onExit, onViewProfile }: { attempt: QuizAttempt; language: Language; onExit: () => void; onViewProfile: () => void }) { const { t } = useI18n(); return <section className="quiz-result"><p className="kicker">{t("viewer.quizCompleted")}</p><h1>{t("common.points", { count: attempt.score })}</h1><p className="quiz-result__accuracy">{quizResultCorrectAnswerSummary(attempt, language)}</p><p>{quizResultXpMessage(attempt.earnedXp, language)}</p><div><button className="quiz-start" onClick={onViewProfile}>{t("viewer.goToProfile")}</button><button className="text-button" onClick={onExit}>{t("viewer.backToDiscover")}</button></div></section>; }

function Profile({ api }: { api: PublicApi }) { const { language, t } = useI18n(); const [data, setData] = useState<{ xp: XpSummary; leaderboard: Leaderboard } | null>(null); const [error, setError] = useState<PublicApiRequestError | null>(null); useEffect(() => { let active = true; Promise.all([api.getXp(), api.getGlobalLeaderboard()]).then(([xp, leaderboard]) => { if (active) setData({ xp, leaderboard }); }).catch((reason: unknown) => { if (active) setError(asPublicError(reason)); }); return () => { active = false; }; }, [api]); return <section className="detail-section profile-page"><p className="kicker">{t("viewer.profile")}</p><h1 className="story-title">{t("viewer.progressArea")}</h1><ErrorNotice error={error} />{data ? <><div className="profile-stats"><article><span>{t("viewer.totalXp")}</span><strong>{data.xp.totalXp}</strong></article><article><span>{t("viewer.completedTransactions")}</span><strong>{data.xp.transactionCount}</strong></article><article><span>{t("viewer.globalRank")}</span><strong>{data.leaderboard.currentUser ? `#${data.leaderboard.currentUser.position}` : "—"}</strong></article></div><p className="profile-note">{t("viewer.profileStaleNotice")}</p><section className="profile-ranking" aria-labelledby="profile-ranking-heading"><div className="section-heading"><div><p className="kicker">{t("viewer.allTime")}</p><h2 id="profile-ranking-heading">{t("viewer.globalLeaderboard")}</h2></div><p>{t("viewer.participants", { count: data.leaderboard.participantCount })}</p></div><p className="leaderboard-meta">{t(data.leaderboard.dataSource === "REDIS" ? "viewer.updatedView" : "viewer.verifiedSource")}</p><ol className="leaderboard-list">{data.leaderboard.leaders.map((entry) => <li className={entry.currentUser ? "current-user" : ""} key={entry.userId}><span>#{entry.position}</span><strong className="leaderboard-name">{leaderboardEntryName(entry, language)}</strong><strong>{entry.totalXp} XP</strong></li>)}</ol>{data.leaderboard.currentUser && !data.leaderboard.leaders.some((entry) => entry.currentUser) && <p className="own-rank">{t("viewer.yourRank", { position: data.leaderboard.currentUser.position, xp: data.leaderboard.currentUser.totalXp })}</p>}</section></> : <p className="loading-copy">{t("viewer.profilePreparing")}</p>}</section>; }
function Cover({ api, content, prominent = false }: { api: PublicApi; content: PublicContentSummary; prominent?: boolean }) { const className = `viewer-cover ${prominent ? "viewer-cover--prominent" : ""} viewer-cover--${content.contentType.toLowerCase()}`; return <div className={className}>{content.coverImageUrl ? <AuthenticatedImage api={api} src={content.coverImageUrl} alt={content.coverAlternativeText || ""} fallback={<span aria-hidden="true">{content.title.slice(0, 1)}</span>} /> : <span aria-hidden="true">{content.title.slice(0, 1)}</span>}</div>; }
function AuthenticatedImage({ api, src, alt, fallback }: { api: PublicApi; src: string; alt: string; fallback: ReactNode }) { const [objectUrl, setObjectUrl] = useState<string | null>(null); const [failed, setFailed] = useState(false); useEffect(() => { let active = true; let createdUrl: string | null = null; setObjectUrl(null); setFailed(false); api.getMedia(src).then((blob) => { if (!active) return; createdUrl = URL.createObjectURL(blob); setObjectUrl(createdUrl); }).catch(() => { if (active) setFailed(true); }); return () => { active = false; if (createdUrl) URL.revokeObjectURL(createdUrl); }; }, [api, src]); if (failed) return fallback; return objectUrl ? <img src={objectUrl} alt={alt} /> : <span className="media-loading" aria-hidden="true" />; }
function Pagination({ page, onPageChange }: { page: PublicContentPage; onPageChange: (page: number) => void }) { const { t } = useI18n(); return <nav className="viewer-pagination" aria-label={t("viewer.contentPages")}><button disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)}>{t("common.previous")}</button><span>{page.page + 1} / {Math.max(page.totalPages, 1)}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>{t("common.next")}</button></nav>; }
function ErrorNotice({ error, retry }: { error: PublicApiRequestError | null; retry?: () => void }) { const { t } = useI18n(); if (!error) return null; return <section className="viewer-error" role="alert"><h2>{t("error.loadTitle")}</h2><p>{error.message}</p><p>{t("error.code")}: <code>{error.code}</code> · {t("error.traceId")}: <code>{error.traceId}</code></p>{retry && <button onClick={retry}>{t("common.retry")}</button>}</section>; }
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
export function leaderboardEntryName(entry: LeaderboardEntry, language: Language = "tr") {
  return entry.displayName?.trim() || translate(language, entry.currentUser ? "viewer.currentUser" : "viewer.user");
}
function asPublicError(reason: unknown): PublicApiRequestError { return reason instanceof PublicApiRequestError ? reason : new PublicApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: translate(readStoredLanguage(), "error.unexpectedClient"), traceId: "unavailable", status: 0 }); }
