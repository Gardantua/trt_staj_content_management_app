import { useEffect, useMemo, useState } from "react";
import { ApiClient, ApiRequestError } from "./api/client";
import { ContentApi } from "./api/content-api";
import { MediaApi } from "./api/media-api";
import { canManageContent, localActorFromEnvironment } from "./auth/actor";
import { ApiErrorNotice } from "./components/ApiErrorNotice";
import { ContentForm } from "./components/ContentForm";
import { CoverEditor } from "./components/CoverEditor";
import { SeasonEditor } from "./components/SeasonEditor";
import type { Content, ContentPage, ContentSummary } from "./domain/content";

const PAGE_SIZE = 20;

export function App() {
  const actor = useMemo(localActorFromEnvironment, []);
  const apiClient = useMemo(() => new ApiClient(actor), [actor]);
  const api = useMemo(() => new ContentApi(apiClient), [apiClient]);
  const mediaApi = useMemo(() => new MediaApi(apiClient), [apiClient]);
  const [path, setPath] = useState(window.location.pathname);

  useEffect(() => {
    const handlePopState = () => setPath(window.location.pathname);
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  function navigate(nextPath: string) {
    window.history.pushState({}, "", nextPath);
    setPath(nextPath);
    window.scrollTo({ top: 0 });
  }

  if (!canManageContent(actor)) {
    return <AccessDenied actorId={actor.id} roles={actor.roles} />;
  }
  const detailMatch = path.match(/^\/contents\/([\w-]+)$/);
  return <main className="app-shell">
    <header className="app-header">
      <a className="brand" href="/" onClick={(event) => { event.preventDefault(); navigate("/"); }} aria-label="İçerik Stüdyosu ana sayfası"><span><strong>İçerik</strong> Stüdyosu</span></a>
      <nav className="primary-nav" aria-label="Ana menü">
        <a className={path === "/" ? "is-active" : ""} href="/" onClick={(event) => { event.preventDefault(); navigate("/"); }}>Katalog</a>
        <a className={path === "/contents/new" ? "is-active" : ""} href="/contents/new" onClick={(event) => { event.preventDefault(); navigate("/contents/new"); }}>Yeni taslak</a>
      </nav>
      <p className="actor-context"><span>Yerel oturum</span><code>{actor.id}</code><strong>{actor.roles.join(", ")}</strong></p>
    </header>
    {path === "/contents/new" ? <CreateContent api={api} navigate={navigate} /> : detailMatch
      ? <ContentDetail api={api} mediaApi={mediaApi} contentId={detailMatch[1]} navigate={navigate} />
      : <ContentList api={api} navigate={navigate} />}
  </main>;
}

function AccessDenied({ actorId, roles }: { actorId: string | null; roles: string[] }) {
  return <main className="app-shell"><section className="access-denied" role="alert">
    <h1>Yönetim erişimi gerekli</h1>
    <p>Bu arayüz yalnız EDITOR veya ADMIN rolü için açılır. Tarayıcıdaki bu kontrol kolaylık içindir; API de isteği bağımsız olarak yetkilendirir.</p>
    <dl><div><dt>Aktör kimliği</dt><dd>{actorId ?? "tanımlı değil"}</dd></div><div><dt>Roller</dt><dd>{roles.join(", ") || "tanımlı değil"}</dd></div></dl>
    <p>Yerel çalışmada <code>VITE_LOCAL_ACTOR_ID</code> ve <code>VITE_LOCAL_ACTOR_ROLES=EDITOR</code> tanımlayın.</p>
  </section></main>;
}

function ContentList({ api, navigate }: { api: ContentApi; navigate: (path: string) => void }) {
  const [page, setPage] = useState<ContentPage | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reloadVersion, setReloadVersion] = useState(0);

  useEffect(() => {
    let isCurrent = true;
    setIsLoading(true); setError(null);
    api.list(currentPage, PAGE_SIZE).then((result) => {
      if (isCurrent) setPage(result);
    }).catch((reason: unknown) => {
      if (isCurrent) setError(asApiError(reason));
    }).finally(() => { if (isCurrent) setIsLoading(false); });
    return () => { isCurrent = false; };
  }, [api, currentPage, reloadVersion]);

  return <section>
    <div className="page-intro">
      <div><p className="eyebrow">Katalog yönetimi</p><h1>İçerik kataloğu</h1><p>İçerik, sezon ve bölüm kayıtlarını yönetin.</p></div>
      <button className="button-primary" onClick={() => navigate("/contents/new")}>Yeni taslak <span aria-hidden="true">→</span></button>
    </div>
    <ApiErrorNotice error={error} onRetry={error ? () => setReloadVersion((version) => version + 1) : undefined} />
    {isLoading ? <p aria-live="polite">İçerikler yükleniyor…</p> : page && <>
      <div className="catalogue-meta"><p>Toplam <strong>{page.totalItems}</strong> içerik</p><p>Sayfa {page.page + 1} / {Math.max(page.totalPages, 1)}</p></div>
      <ul className="content-list" aria-label="İçerik listesi">
        {page.items.map((content) => <ContentListItem key={content.id} content={content} onOpen={() => navigate(`/contents/${content.id}`)} />)}
      </ul>
      {page.items.length === 0 && <p>Henüz içerik yok. Yeni bir draft oluşturabilirsiniz.</p>}
      <nav className="pagination" aria-label="Sayfalandırma">
        <button disabled={page.page === 0} onClick={() => setCurrentPage(page.page - 1)}>Önceki</button>
        <span>{page.page + 1} / {Math.max(page.totalPages, 1)}</span>
        <button disabled={page.page + 1 >= page.totalPages} onClick={() => setCurrentPage(page.page + 1)}>Sonraki</button>
      </nav>
    </>}
  </section>;
}

function ContentListItem({ content, onOpen }: { content: ContentSummary; onOpen: () => void }) {
  return <li><article className="content-card">
    <div className={`content-poster content-poster--${content.contentType.toLowerCase()}`} aria-hidden="true"><span>{content.contentType === "SERIES" ? "Dizi" : "Film"}</span><strong>{content.title.slice(0, 1)}</strong></div>
    <div className="content-card__body"><div className="content-card__meta"><span className="content-type">{content.contentType === "SERIES" ? "Dizi" : "Film"}</span><span className={`status status--${content.publicationStatus.toLowerCase()}`}>{content.publicationStatus === "DRAFT" ? "Taslak" : "Yayında"}</span></div><h2>{content.title}</h2><p>{content.description || "Henüz kısa açıklama eklenmedi."}</p></div>
    <button className="button-secondary" onClick={onOpen} aria-label={`${content.title} içeriğini aç`}>Aç <span aria-hidden="true">→</span></button>
  </article></li>;
}

function CreateContent({ api, navigate }: { api: ContentApi; navigate: (path: string) => void }) {
  const [error, setError] = useState<ApiRequestError | null>(null);
  return <section className="editor"><div className="editor-heading"><p className="eyebrow">Yeni içerik</p><h1>Yeni içerik oluştur</h1><p>Temel katalog bilgilerini girin. Sezon ve bölümleri kayıttan sonra ekleyin.</p></div><ApiErrorNotice error={error} />
    <ContentForm includeContentType submitLabel="Draft oluştur" onSubmit={async (input) => {
      try { const created = await api.create(input); navigate(`/contents/${created.id}`); } catch (reason) { setError(asApiError(reason)); }
    }} />
  </section>;
}

function ContentDetail({ api, mediaApi, contentId, navigate }: { api: ContentApi; mediaApi: MediaApi; contentId: string; navigate: (path: string) => void }) {
  const [content, setContent] = useState<Content | null>(null);
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isPublishing, setIsPublishing] = useState(false);

  useEffect(() => {
    let isCurrent = true;
    api.get(contentId).then((result) => { if (isCurrent) setContent(result); }).catch((reason) => { if (isCurrent) setError(asApiError(reason)); }).finally(() => { if (isCurrent) setIsLoading(false); });
    return () => { isCurrent = false; };
  }, [api, contentId]);

  if (isLoading) return <p className="loading-copy" aria-live="polite">İçerik yükleniyor…</p>;
  if (!content) return <section><ApiErrorNotice error={error} /><button className="button-secondary" onClick={() => navigate("/")}>Listeye dön</button></section>;
  const editable = content.publicationStatus === "DRAFT";

  return <section className="editor">
    <p className="back-link"><a href="/" onClick={(event) => { event.preventDefault(); navigate("/"); }}>← Kataloğa dön</a></p>
    <div className="detail-hero"><div><div className="content-card__meta"><span className="content-type">{content.contentType === "SERIES" ? "Dizi" : "Film"}</span><span className={`status status--${content.publicationStatus.toLowerCase()}`}>{content.publicationStatus === "DRAFT" ? "Taslak" : "Yayında"}</span></div><h1>{content.title}</h1><p>{content.description || "Henüz kısa açıklama eklenmedi."}</p></div>
      {editable && <button className="button-primary" disabled={isPublishing} onClick={async () => { setError(null); setIsPublishing(true); try { setContent(await api.publish(content.id)); } catch (reason) { setError(asApiError(reason)); } finally { setIsPublishing(false); } }}>{isPublishing ? "Yayınlanıyor…" : "Yayınla"}</button>}
    </div>
    <ApiErrorNotice error={error} />
    {!editable && <p className="notice">Yayınlanmış içerik değiştirilemez. Bu davranış, yayınlanan katalog bilgisinin sabit kalmasını sağlar.</p>}
    {editable && <CoverEditor content={content} onUploadImage={(file) => mediaApi.uploadImage(file)} onBindCover={(input) => api.setCover(content.id, input)} onContentChanged={setContent} onError={(reason) => setError(asApiError(reason))} />}
    {editable && <ContentForm initialValue={{ title: content.title, description: content.description ?? "", contentType: content.contentType }} includeContentType={false} submitLabel="İçeriği kaydet" onSubmit={async (input) => { try { setError(null); setContent(await api.update(content.id, input)); } catch (reason) { setError(asApiError(reason)); } }} />}
    {editable && content.contentType === "SERIES" && <SeasonEditor content={content} onContentChanged={setContent} onError={(reason) => setError(asApiError(reason))}
      onAddSeason={(input) => api.addSeason(content.id, input)} onUpdateSeason={(season, input) => api.updateSeason(content.id, season.id, input)}
      onDeleteSeason={async (season) => { try { setError(null); await api.deleteSeason(content.id, season.id); setContent(await api.get(content.id)); } catch (reason) { setError(asApiError(reason)); } }}
      onAddEpisode={(season, input) => api.addEpisode(content.id, season.id, input)} onUpdateEpisode={(season, episode, input) => api.updateEpisode(content.id, season.id, episode.id, input)}
      onDeleteEpisode={async (season, episode) => { try { setError(null); await api.deleteEpisode(content.id, season.id, episode.id); setContent(await api.get(content.id)); } catch (reason) { setError(asApiError(reason)); } }} />}
    {content.contentType === "FILM" && <p className="notice">Filmler sezon içeremez; bu kural API tarafından da korunur.</p>}
  </section>;
}

function asApiError(reason: unknown): ApiRequestError {
  return reason instanceof ApiRequestError ? reason : new ApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: "Beklenmeyen bir istemci hatası oluştu.", traceId: "unavailable", status: 0 });
}
