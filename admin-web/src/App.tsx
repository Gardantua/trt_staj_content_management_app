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
import { SeasonEditor } from "./components/SeasonEditor";
import type { Content, ContentPage, ContentSummary } from "./domain/content";
import { AuthApi, type AccountSession } from "./user/auth-api";
import { PublicApiRequestError } from "./user/public-api";

const PAGE_SIZE = 20;

export function App() {
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

  if (account === undefined) return <main className="app-shell"><p className="loading-copy">Yönetim oturumu kontrol ediliyor…</p></main>;
  if (!account) return <AdminAccess authApi={authApi} onSignedIn={setAccount} />;
  const actor = { id: account.actorId, roles: account.roles };
  if (!canManageContent(actor)) return <AccessDenied account={account} onSignOut={async () => { await authApi.logout(); setAccount(null); }} />;
  const detailMatch = path.match(/^\/admin\/contents\/([\w-]+)$/);

  return <main className="app-shell">
    <header className="app-header">
      <a className="brand" href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }} aria-label="İçerik yönetimi ana sayfası"><span><strong>İçerik</strong> Stüdyosu</span></a>
      <nav className="primary-nav" aria-label="Ana menü">
        <a className={path === "/admin" || path.startsWith("/admin/contents") ? "is-active" : ""} href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }}>İçerik yönetimi</a>
        <a className={path === "/admin/quizzes" ? "is-active" : ""} href="/admin/quizzes" onClick={(event) => { event.preventDefault(); navigate("/admin/quizzes"); }}>Quizler</a>
        <a className={path === "/admin/quiz-history" ? "is-active" : ""} href="/admin/quiz-history" onClick={(event) => { event.preventDefault(); navigate("/admin/quiz-history"); }}>Quiz geçmişi</a>
      </nav>
      <p className="actor-context"><span>{account.displayName}</span><strong>{account.roles.join(", ")}</strong><button type="button" onClick={async () => { await authApi.logout(); setAccount(null); }}>Çıkış</button></p>
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
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null); setSubmitting(true);
    try { onSignedIn(await authApi.login(email, password)); }
    catch (reason) { setError(reason instanceof PublicApiRequestError ? reason.message : "Giriş yapılamadı."); }
    finally { setSubmitting(false); }
  }

  return <main className="admin-access"><section><p className="eyebrow">İçerik Stüdyosu</p><h1>Yönetim girişi</h1><p>Yalnızca editör veya yönetici hesabınla giriş yap.</p>{error && <p className="form-validation" role="alert">{error}</p>}<form className="stack" onSubmit={submit}><label>E-posta<input type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><label>Şifre<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label><button className="button-primary" disabled={submitting}>{submitting ? "Giriş yapılıyor…" : "Giriş yap"}</button></form><a href="/">Kullanıcı sitesine dön</a></section></main>;
}

function AccessDenied({ account, onSignOut }: { account: AccountSession; onSignOut: () => Promise<void> }) {
  return <main className="app-shell"><section className="access-denied" role="alert">
    <h1>Yönetim erişimi gerekli</h1><p>Bu arayüz yalnız EDITOR veya ADMIN rolü için açılır. API isteği ayrıca yetkilendirir.</p>
    <dl><div><dt>Hesap</dt><dd>{account.email}</dd></div><div><dt>Roller</dt><dd>{account.roles.join(", ") || "tanımlı değil"}</dd></div></dl><button className="button-secondary" onClick={() => void onSignOut()}>Çıkış yap</button>
  </section></main>;
}

function ContentManagement({ api, mediaApi, navigate }: { api: ContentApi; mediaApi: MediaApi; navigate: (path: string) => void }) {
  const [error, setError] = useState<ApiRequestError | null>(null);
  return <section>
    <div className="editor-heading"><p className="eyebrow">Dizi ve film işlemleri</p><h1>İçerik yönetimi</h1><p>Yeni bir dizi veya film ekleyin; mevcut içeriğin bilgilerini ve kapağını düzenleyin.</p></div>
    <ApiErrorNotice error={error} />
    <details className="content-create-panel"><summary>Yeni dizi veya film ekle</summary><ContentForm includeContentType submitLabel="İçeriği oluştur" onSubmit={async (input) => {
      try { navigate(`/admin/contents/${(await api.create(input)).id}`); } catch (reason) { setError(asApiError(reason)); }
    }} /></details>
    <ContentList api={api} mediaApi={mediaApi} navigate={navigate} />
  </section>;
}

function ContentList({ api, mediaApi, navigate }: { api: ContentApi; mediaApi: MediaApi; navigate: (path: string) => void }) {
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
    <div className="list-heading"><h2>Mevcut içerikler</h2><p>Düzenlemek için bir içerik açın.</p></div>
    <div className="content-search" role="search">
      <label htmlFor="content-title-search">İsme göre içerik ara</label>
      <div><input id="content-title-search" type="search" maxLength={200} value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Dizi veya film adı yazın" />
      {searchQuery ? <button className="button-secondary" type="button" onClick={() => setSearchQuery("")}>Temizle</button> : null}</div>
      <p>Arama bütün içeriklerde yapılır; sonuçlar sayfalanarak gösterilir.</p>
    </div>
    <ApiErrorNotice error={error} onRetry={error ? () => setReloadVersion((version) => version + 1) : undefined} />
    {isLoading ? <p aria-live="polite">İçerikler yükleniyor…</p> : page ? <>
      <div className="catalogue-meta" aria-live="polite"><p>{activeSearchQuery ? <><strong>{page.totalItems}</strong> eşleşen içerik</> : <>Toplam <strong>{page.totalItems}</strong> içerik</>}</p><p>Sayfa {page.page + 1} / {Math.max(page.totalPages, 1)}</p></div>
      <ul className="content-list" aria-label="İçerik listesi">{page.items.map((content) => <ContentListItem key={content.id} content={content} mediaApi={mediaApi} onOpen={() => navigate(`/admin/contents/${content.id}`)} />)}</ul>
      {page.items.length === 0 ? <p>{activeSearchQuery ? `“${activeSearchQuery}” adına uyan içerik bulunamadı.` : "Henüz içerik yok. İlk dizi veya filmi yukarıdaki formdan ekleyebilirsiniz."}</p> : null}
      <nav className="pagination" aria-label="Sayfalandırma"><button disabled={page.page === 0} onClick={() => setCurrentPage(page.page - 1)}>Önceki</button><span>{page.page + 1} / {Math.max(page.totalPages, 1)}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => setCurrentPage(page.page + 1)}>Sonraki</button></nav>
    </> : null}
  </section>;
}

function ContentListItem({ content, mediaApi, onOpen }: { content: ContentSummary; mediaApi: MediaApi; onOpen: () => void }) {
  return <li><article className="content-card">
    <div className={`content-poster content-poster--${content.contentType.toLowerCase()}`}>{content.coverImageUrl
      ? <ProtectedImage contentUrl={content.coverImageUrl} alternativeText={content.coverAlternativeText ?? `${content.title} kapağı`} mediaApi={mediaApi} />
      : <><span>{content.contentType === "SERIES" ? "Dizi" : "Film"}</span><strong aria-hidden="true">{content.title.slice(0, 1)}</strong></>}</div>
    <div className="content-card__body"><div className="content-card__meta"><span className="content-type">{content.contentType === "SERIES" ? "Dizi" : "Film"}</span></div><h2>{content.title}</h2><p>{content.description || "Henüz kısa açıklama eklenmedi."}</p></div>
    <button className="button-secondary" onClick={onOpen} aria-label={`${content.title} içeriğini aç`}>Aç <span aria-hidden="true">→</span></button>
  </article></li>;
}

function ContentDetail({ api, mediaApi, quizApi, contentId, navigate }: { api: ContentApi; mediaApi: MediaApi; quizApi: QuizApi; contentId: string; navigate: (path: string) => void }) {
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

  if (isLoading) return <p className="loading-copy" aria-live="polite">İçerik yükleniyor…</p>;
  if (!content) return <section><ApiErrorNotice error={error} /><button className="button-secondary" onClick={() => navigate("/admin/contents")}>Listeye dön</button></section>;
  const editableHierarchy = content.publicationStatus === "DRAFT";

  return <section className="editor">
    <p className="back-link"><a href="/admin/contents" onClick={(event) => { event.preventDefault(); navigate("/admin/contents"); }}>← İçerik yönetimine dön</a></p>
    <div className="detail-hero"><div className="detail-cover">{content.coverImageUrl ? <ProtectedImage contentUrl={content.coverImageUrl} alternativeText={content.coverAlternativeText ?? `${content.title} kapağı`} mediaApi={mediaApi} /> : <span aria-hidden="true">{content.title.slice(0, 1)}</span>}</div><div><div className="content-card__meta"><span className="content-type">{content.contentType === "SERIES" ? "Dizi" : "Film"}</span></div><h1>{content.title}</h1><p>{content.description || "Henüz kısa açıklama eklenmedi."}</p></div>
      <div className="detail-actions">{editableHierarchy ? <button className="button-primary" disabled={isPublishing || isDeleting} onClick={async () => { setError(null); setIsPublishing(true); try { setContent(await api.publish(content.id)); } catch (reason) { setError(asApiError(reason)); } finally { setIsPublishing(false); } }}>{isPublishing ? "Kataloğa ekleniyor…" : "Kataloğa ekle"}</button> : null}<button className="button-danger" disabled={isPublishing || isDeleting} onClick={async () => { if (!window.confirm(`“${content.title}” içeriği, sezonları, bölümleri ve bağlı quizleriyle birlikte kalıcı olarak silinecek. Bu işlem geri alınamaz. Devam edilsin mi?`)) return; setError(null); setIsDeleting(true); try { await api.deleteContent(content.id); navigate("/admin/contents"); } catch (reason) { setError(asApiError(reason)); setIsDeleting(false); } }}>{isDeleting ? "Siliniyor…" : "İçeriği kalıcı sil"}</button></div>
    </div>
    <ApiErrorNotice error={error} />
    <section className="publication-guide" aria-label="Kullanıcı sayfası yayın durumu">
      <strong>Kullanıcı sayfasında görünürlük</strong>
      <span className={content.publicationStatus === "PUBLISHED" ? "is-complete" : ""}>1. İçeriği kataloğa ekle {content.publicationStatus === "PUBLISHED" ? "✓" : ""}</span>
      <span>2. Quizler sekmesinden hazırlanan quizi kullanıma aç</span>
    </section>
    <nav className="detail-tabs" aria-label="İçerik çalışma alanları"><button type="button" className={activeTab === "content" ? "is-active" : ""} onClick={() => { setError(null); setActiveTab("content"); }}>İçerik bilgileri</button><button type="button" className={activeTab === "quiz" ? "is-active" : ""} onClick={() => { setError(null); setActiveTab("quiz"); }}>Quizler</button></nav>
    {activeTab === "content" ? <>
      {!editableHierarchy ? <p className="notice notice--info">Başlık, açıklama ve kapak güncellenebilir. Dizilere yeni sezon ve bölüm eklenebilir; mevcut sezon ve bölümler korunur.</p> : <p className="notice notice--info">Kapak ve içerik bilgilerini tamamlayın. Dizilerde sezon ve bölümleri ekledikten sonra içeriği kataloğa ekleyin.</p>}
      <CoverEditor content={content} onUploadImage={(file) => mediaApi.uploadImage(file)} onBindCover={(input) => api.setCover(content.id, input)} onContentChanged={setContent} onError={handleError} />
      <ContentForm initialValue={{ title: content.title, description: content.description ?? "", contentType: content.contentType }} includeContentType={false} submitLabel="İçerik bilgilerini kaydet" onSubmit={async (input) => { try { setError(null); setContent(await api.update(content.id, input)); } catch (reason) { setError(asApiError(reason)); } }} />
      {content.contentType === "SERIES" ? <SeasonEditor content={content} onContentChanged={setContent} onError={handleError}
        onAddSeasonPlan={(input) => api.addSeasonPlan(content.id, input)} onUpdateSeason={(season, input) => api.updateSeason(content.id, season.id, input)}
        onAddEpisode={(season, input) => api.addEpisode(content.id, season.id, input)}
        onDeleteSeason={async (season) => { await api.deleteSeason(content.id, season.id); setContent(await api.get(content.id)); }}
        onUpdateEpisode={(season, episode, input) => api.updateEpisode(content.id, season.id, episode.id, input)}
        onDeleteEpisode={async (season, episode) => { await api.deleteEpisode(content.id, season.id, episode.id); setContent(await api.get(content.id)); }} /> : null}
      {content.contentType === "FILM" ? <p className="notice notice--info">Filmler sezon içeremez.</p> : null}
    </> : <QuizStudio content={content} quizApi={quizApi} mediaApi={mediaApi} initialQuizId={new URLSearchParams(window.location.search).get("quiz")} onError={handleError} />}
  </section>;
}

function asApiError(reason: unknown): ApiRequestError {
  return reason instanceof ApiRequestError ? reason : new ApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: "Beklenmeyen bir istemci hatası oluştu.", traceId: "unavailable", status: 0 });
}
