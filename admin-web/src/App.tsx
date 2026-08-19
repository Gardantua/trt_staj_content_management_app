import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiClient, ApiRequestError } from "./api/client";
import { ContentApi } from "./api/content-api";
import { MediaApi } from "./api/media-api";
import { QuizApi } from "./api/quiz-api";
import { canManageContent } from "./auth/actor";
import { ApiErrorNotice } from "./components/ApiErrorNotice";
import { ContentForm } from "./components/ContentForm";
import { CoverEditor } from "./components/CoverEditor";
import { ProtectedImage } from "./components/ProtectedImage";
import { QuizOverview } from "./components/QuizOverview";
import { QuizStudio } from "./components/QuizStudio";
import { ContentTranslationEditor } from "./components/TranslationEditors";
import { SeasonEditor } from "./components/SeasonEditor";
import type { Content, ContentPage, ContentSummary } from "./domain/content";
import { readStoredLanguage, translate, useI18n } from "./i18n/I18nContext";
import { LanguageSwitcher } from "./i18n/LanguageSwitcher";
import { AuthApi, type AccountSession } from "./user/auth-api";
import { PublicApiRequestError } from "./user/public-api";

const PAGE_SIZE = 20;

export function App() {
  const { t } = useI18n();
  const authApi = useMemo(() => new AuthApi(), []);
  const apiClient = useMemo(() => new ApiClient(), []);
  const contentApi = useMemo(() => new ContentApi(apiClient), [apiClient]);
  const mediaApi = useMemo(() => new MediaApi(apiClient), [apiClient]);
  const quizApi = useMemo(() => new QuizApi(apiClient), [apiClient]);
  const [path, setPath] = useState(window.location.pathname);
  const [account, setAccount] = useState<AccountSession | null | undefined>(undefined);

  useEffect(() => {
    let active = true;
    authApi.me()
      .then((session) => { if (active) setAccount(session); })
      .catch(() => { if (active) setAccount(null); });
    return () => { active = false; };
  }, [authApi]);

  useEffect(() => {
    const handlePopState = () => setPath(window.location.pathname);
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  function navigate(nextPath: string) {
    window.history.pushState({}, "", nextPath);
    setPath(window.location.pathname);
    window.scrollTo({ top: 0 });
  }

  if (account === undefined) return <main className="app-shell"><LanguageSwitcher /><p className="loading-copy">{t("admin.sessionChecking")}</p></main>;
  if (!account) return <AdminAccess authApi={authApi} onSignedIn={setAccount} />;
  const actor = { id: account.actorId, roles: account.roles };
  if (!canManageContent(actor)) return <AccessDenied account={account} onSignOut={async () => { await authApi.logout(); setAccount(null); }} />;
  const detailMatch = path.match(/^\/admin\/contents\/([\w-]+)$/);

  return <main className="app-shell">
    <header className="app-header">
      <a className="brand" href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }} aria-label={t("admin.homeAria")}><span>{t("admin.brand")}</span></a>
      <nav className="primary-nav" aria-label={t("admin.mainMenu")}>
        <a className={path === "/admin" || path.startsWith("/admin/contents") ? "is-active" : ""} href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }}>{t("admin.contentManagement")}</a>
        <a className={path === "/admin/quizzes" ? "is-active" : ""} href="/admin/quizzes" onClick={(event) => { event.preventDefault(); navigate("/admin/quizzes"); }}>{t("admin.quizzes")}</a>
        <a className={path === "/admin/quiz-history" ? "is-active" : ""} href="/admin/quiz-history" onClick={(event) => { event.preventDefault(); navigate("/admin/quiz-history"); }}>{t("admin.quizHistory")}</a>
      </nav>
      <div className="app-header__actions">
        <LanguageSwitcher />
        <p className="actor-context"><span>{account.displayName}</span><strong>{account.roles.join(", ")}</strong><button type="button" onClick={async () => { await authApi.logout(); setAccount(null); }}>{t("admin.signOut")}</button></p>
      </div>
    </header>
    {detailMatch
      ? <ContentDetail api={contentApi} mediaApi={mediaApi} quizApi={quizApi} contentId={detailMatch[1]} navigate={navigate} />
      : path === "/admin/quizzes"
        ? <QuizOverview mode="active" contentApi={contentApi} quizApi={quizApi} mediaApi={mediaApi} navigate={navigate} />
        : path === "/admin/quiz-history"
          ? <QuizOverview mode="history" contentApi={contentApi} quizApi={quizApi} mediaApi={mediaApi} navigate={navigate} />
      : <ContentManagement api={contentApi} mediaApi={mediaApi} navigate={navigate} />}
  </main>;
}

function AdminAccess({ authApi, onSignedIn }: { authApi: AuthApi; onSignedIn: (account: AccountSession) => void }) {
  const { t } = useI18n();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null); setSubmitting(true);
    try { onSignedIn(await authApi.login(email, password)); }
    catch (reason) { setError(reason instanceof PublicApiRequestError ? reason.message : t("admin.loginFailed")); }
    finally { setSubmitting(false); }
  }

  return <main className="admin-access"><LanguageSwitcher /><section><p className="eyebrow">{t("admin.brand")}</p><h1>{t("admin.loginTitle")}</h1><p>{t("admin.loginHelp")}</p>{error && <p className="form-validation" role="alert">{error}</p>}<form className="stack" onSubmit={submit}><label>{t("common.email")}<input type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><label>{t("common.password")}<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label><button className="button-primary" disabled={submitting}>{submitting ? t("admin.signingIn") : t("admin.signIn")}</button></form><a href="/">{t("admin.backToUserSite")}</a></section></main>;
}

function AccessDenied({ account, onSignOut }: { account: AccountSession; onSignOut: () => Promise<void> }) {
  const { t } = useI18n();
  return <main className="app-shell"><section className="access-denied" role="alert">
    <LanguageSwitcher /><h1>{t("admin.accessDeniedTitle")}</h1><p>{t("admin.accessDeniedBody")}</p>
    <dl><div><dt>{t("admin.account")}</dt><dd>{account.email}</dd></div><div><dt>{t("admin.roles")}</dt><dd>{account.roles.join(", ") || t("common.notDefined")}</dd></div></dl><button className="button-secondary" onClick={() => void onSignOut()}>{t("admin.signOutAction")}</button>
  </section></main>;
}

function ContentManagement({ api, mediaApi, navigate }: { api: ContentApi; mediaApi: MediaApi; navigate: (path: string) => void }) {
  const { t } = useI18n();
  const [error, setError] = useState<ApiRequestError | null>(null);
  return <section>
    <div className="editor-heading"><p className="eyebrow">{t("admin.contentEyebrow")}</p><h1>{t("admin.contentManagement")}</h1><p>{t("admin.contentIntro")}</p></div>
    <ApiErrorNotice error={error} />
    <details className="content-create-panel"><summary>{t("admin.addContent")}</summary><ContentForm includeContentType submitLabel={t("admin.createContent")} onSubmit={async (input) => {
      try { navigate(`/admin/contents/${(await api.create(input)).id}`); } catch (reason) { setError(asApiError(reason)); }
    }} /></details>
    <ContentList api={api} mediaApi={mediaApi} navigate={navigate} />
  </section>;
}

function ContentList({ api, mediaApi, navigate }: { api: ContentApi; mediaApi: MediaApi; navigate: (path: string) => void }) {
  const { t } = useI18n();
  const [page, setPage] = useState<ContentPage | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeSearchQuery, setActiveSearchQuery] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setCurrentPage(0);
      setActiveSearchQuery(searchQuery.trim());
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    let isCurrent = true;
    setIsLoading(true); setError(null);
    api.list(currentPage, PAGE_SIZE, activeSearchQuery).then((result) => { if (isCurrent) setPage(result); })
      .catch((reason: unknown) => { if (isCurrent) setError(asApiError(reason)); })
      .finally(() => { if (isCurrent) setIsLoading(false); });
    return () => { isCurrent = false; };
  }, [activeSearchQuery, api, currentPage, reloadVersion]);

  return <section className="management-list">
    <div className="list-heading"><h2>{t("admin.existingContents")}</h2><p>{t("admin.openToEdit")}</p></div>
    <div className="content-search" role="search">
      <label htmlFor="content-title-search">{t("admin.searchContent")}</label>
      <div><input id="content-title-search" type="search" maxLength={200} value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder={t("admin.searchPlaceholder")} />
      {searchQuery ? <button className="button-secondary" type="button" onClick={() => setSearchQuery("")}>{t("common.clear")}</button> : null}</div>
      <p>{t("admin.searchHelp")}</p>
    </div>
    <ApiErrorNotice error={error} onRetry={error ? () => setReloadVersion((version) => version + 1) : undefined} />
    {isLoading ? <p aria-live="polite">{t("admin.contentsLoading")}</p> : page ? <>
      <div className="catalogue-meta" aria-live="polite"><p>{t(activeSearchQuery ? "admin.matchingContents" : "admin.totalContents", { count: page.totalItems })}</p><p>{t("common.page", { current: page.page + 1, total: Math.max(page.totalPages, 1) })}</p></div>
      <ul className="content-list" aria-label={t("admin.contentList")}>{page.items.map((content) => <ContentListItem key={content.id} content={content} mediaApi={mediaApi} onOpen={() => navigate(`/admin/contents/${content.id}`)} />)}</ul>
      {page.items.length === 0 ? <p>{activeSearchQuery ? t("admin.noSearchResults", { query: activeSearchQuery }) : t("admin.noContents")}</p> : null}
      <nav className="pagination" aria-label={t("admin.pagination")}><button disabled={page.page === 0} onClick={() => setCurrentPage(page.page - 1)}>{t("common.previous")}</button><span>{page.page + 1} / {Math.max(page.totalPages, 1)}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => setCurrentPage(page.page + 1)}>{t("common.next")}</button></nav>
    </> : null}
  </section>;
}

function ContentListItem({ content, mediaApi, onOpen }: { content: ContentSummary; mediaApi: MediaApi; onOpen: () => void }) {
  const { t } = useI18n();
  return <li><article className="content-card">
    <div className={`content-poster content-poster--${content.contentType.toLowerCase()}`}>{content.coverImageUrl
      ? <ProtectedImage contentUrl={content.coverImageUrl} alternativeText={content.coverAlternativeText ?? t("admin.coverAlt", { title: content.title })} mediaApi={mediaApi} />
      : <><span>{t(content.contentType === "SERIES" ? "common.series" : "common.film")}</span><strong aria-hidden="true">{content.title.slice(0, 1)}</strong></>}</div>
    <div className="content-card__body"><div className="content-card__meta"><span className="content-type">{t(content.contentType === "SERIES" ? "common.series" : "common.film")}</span></div><h2>{content.title}</h2><p>{content.description || t("admin.noDescription")}</p></div>
    <button className="button-secondary" onClick={onOpen} aria-label={t("admin.openContentAria", { title: content.title })}>{t("common.open")} <span aria-hidden="true">→</span></button>
  </article></li>;
}

function ContentDetail({ api, mediaApi, quizApi, contentId, navigate }: { api: ContentApi; mediaApi: MediaApi; quizApi: QuizApi; contentId: string; navigate: (path: string) => void }) {
  const { t } = useI18n();
  const [content, setContent] = useState<Content | null>(null);
  const [activeTab, setActiveTab] = useState<"content" | "quiz">(() => new URLSearchParams(window.location.search).get("tab") === "quiz" ? "quiz" : "content");
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const handleError = useCallback((reason: unknown) => setError(asApiError(reason)), []);

  useEffect(() => {
    let isCurrent = true;
    api.get(contentId).then((result) => { if (isCurrent) setContent(result); })
      .catch((reason) => { if (isCurrent) setError(asApiError(reason)); })
      .finally(() => { if (isCurrent) setIsLoading(false); });
    return () => { isCurrent = false; };
  }, [api, contentId]);

  if (isLoading) return <p className="loading-copy" aria-live="polite">{t("admin.contentLoading")}</p>;
  if (!content) return <section><ApiErrorNotice error={error} /><button className="button-secondary" onClick={() => navigate("/admin/contents")}>{t("admin.backToList")}</button></section>;
  const editableHierarchy = content.publicationStatus === "DRAFT";

  return <section className="editor">
    <p className="back-link"><a href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }}>← {t("admin.backToContent")}</a></p>
    <div className="detail-hero"><div className="detail-cover">{content.coverImageUrl ? <ProtectedImage contentUrl={content.coverImageUrl} alternativeText={content.coverAlternativeText ?? t("admin.coverAlt", { title: content.title })} mediaApi={mediaApi} /> : <span aria-hidden="true">{content.title.slice(0, 1)}</span>}</div><div><div className="content-card__meta"><span className="content-type">{t(content.contentType === "SERIES" ? "common.series" : "common.film")}</span></div><h1>{content.title}</h1><p>{content.description || t("admin.noDescription")}</p></div>
      <div className="detail-actions">{editableHierarchy ? <button className="button-primary" disabled={isPublishing || isDeleting} onClick={async () => { setError(null); setIsPublishing(true); try { setContent(await api.publish(content.id)); } catch (reason) { setError(asApiError(reason)); } finally { setIsPublishing(false); } }}>{isPublishing ? t("admin.addingToCatalog") : t("admin.addToCatalog")}</button> : null}<button className="button-danger" disabled={isPublishing || isDeleting} onClick={async () => { if (!window.confirm(t("admin.deleteConfirm", { title: content.title }))) return; setError(null); setIsDeleting(true); try { await api.deleteContent(content.id); navigate("/admin/contents"); } catch (reason) { setError(asApiError(reason)); setIsDeleting(false); } }}>{isDeleting ? t("admin.deleting") : t("admin.deletePermanently")}</button></div>
    </div>
    <ApiErrorNotice error={error} />
    <section className="publication-guide" aria-label={t("admin.publicationStatusAria")}>
      <strong>{t("admin.publicationVisibility")}</strong>
      <span className={content.publicationStatus === "PUBLISHED" ? "is-complete" : ""}>{t("admin.publicationStepContent")} {content.publicationStatus === "PUBLISHED" ? "✓" : ""}</span>
      <span>{t("admin.publicationStepQuiz")}</span>
    </section>
    <nav className="detail-tabs" aria-label={t("admin.workspaces")}><button type="button" className={activeTab === "content" ? "is-active" : ""} onClick={() => { setError(null); setActiveTab("content"); }}>{t("admin.contentInformation")}</button><button type="button" className={activeTab === "quiz" ? "is-active" : ""} onClick={() => { setError(null); setActiveTab("quiz"); }}>{t("admin.quizzes")}</button></nav>
    {activeTab === "content" ? <>
      {!editableHierarchy ? <p className="notice notice--info">{t("admin.publishedEditNotice")}</p> : <p className="notice notice--info">{t("admin.draftEditNotice")}</p>}
      <CoverEditor content={content} onUploadImage={(file) => mediaApi.uploadImage(file)} onBindCover={(input) => api.setCover(content.id, input)} onContentChanged={setContent} onError={handleError} />
      <ContentForm initialValue={{ title: content.title, description: content.description ?? "", contentType: content.contentType }} includeContentType={false} submitLabel={t("admin.saveContentInformation")} onSubmit={async (input) => { try { setError(null); setContent(await api.update(content.id, input)); } catch (reason) { setError(asApiError(reason)); } }} />
      {content.contentType === "SERIES" ? <SeasonEditor content={content} onContentChanged={setContent} onError={handleError}
        onAddSeasonPlan={(input) => api.addSeasonPlan(content.id, input)} onUpdateSeason={(season, input) => api.updateSeason(content.id, season.id, input)}
        onAddEpisode={(season, input) => api.addEpisode(content.id, season.id, input)}
        onDeleteSeason={async (season) => { await api.deleteSeason(content.id, season.id); setContent(await api.get(content.id)); }}
        onUpdateEpisode={(season, episode, input) => api.updateEpisode(content.id, season.id, episode.id, input)}
        onDeleteEpisode={async (season, episode) => { await api.deleteEpisode(content.id, season.id, episode.id); setContent(await api.get(content.id)); }} /> : null}
      {content.contentType === "FILM" ? <p className="notice notice--info">{t("admin.filmHasNoSeasons")}</p> : null}
      <ContentTranslationEditor content={content} api={api} onError={handleError} />
    </> : <QuizStudio content={content} quizApi={quizApi} mediaApi={mediaApi} initialQuizId={new URLSearchParams(window.location.search).get("quiz")} onError={handleError} />}
  </section>;
}

function asApiError(reason: unknown): ApiRequestError {
  return reason instanceof ApiRequestError ? reason : new ApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: translate(readStoredLanguage(), "error.unexpectedClient"), traceId: "unavailable", status: 0 });
}
