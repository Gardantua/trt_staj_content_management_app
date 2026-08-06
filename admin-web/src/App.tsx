import { useEffect, useMemo, useState } from "react";
import { ApiClient, ApiRequestError } from "./api/client";
import { ContentApi } from "./api/content-api";
import { canManageContent, localActorFromEnvironment } from "./auth/actor";
import { ApiErrorNotice } from "./components/ApiErrorNotice";
import { ContentForm } from "./components/ContentForm";
import { SeasonEditor } from "./components/SeasonEditor";
import type { Content, ContentPage, ContentSummary } from "./domain/content";

const PAGE_SIZE = 20;

export function App() {
  const actor = useMemo(localActorFromEnvironment, []);
  const api = useMemo(() => new ContentApi(new ApiClient(actor)), [actor]);
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
      <a href="/" onClick={(event) => { event.preventDefault(); navigate("/"); }}>İçerik Yönetimi</a>
      <p>Yerel aktör: <code>{actor.id}</code> · roller: <strong>{actor.roles.join(", ")}</strong></p>
    </header>
    {path === "/contents/new" ? <CreateContent api={api} navigate={navigate} /> : detailMatch
      ? <ContentDetail api={api} contentId={detailMatch[1]} navigate={navigate} />
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
    <div className="title-row"><div><h1>İçerikler</h1><p>Draft içerikler burada düzenlenir; yayın kararı backend iş kurallarıyla doğrulanır.</p></div><button onClick={() => navigate("/contents/new")}>Yeni draft</button></div>
    <ApiErrorNotice error={error} onRetry={error ? () => setReloadVersion((version) => version + 1) : undefined} />
    {isLoading ? <p aria-live="polite">İçerikler yükleniyor…</p> : page && <>
      <p className="muted">Toplam {page.totalItems} içerik</p>
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
    <div><p className="eyebrow">{content.contentType === "SERIES" ? "Dizi" : "Film"} · {content.publicationStatus === "DRAFT" ? "Draft" : "Yayınlandı"}</p><h2>{content.title}</h2><p>{content.description || "Açıklama yok"}</p></div>
    <button onClick={onOpen} aria-label={`${content.title} içeriğini düzenle`}>Aç</button>
  </article></li>;
}

function CreateContent({ api, navigate }: { api: ContentApi; navigate: (path: string) => void }) {
  const [error, setError] = useState<ApiRequestError | null>(null);
  return <section className="editor"><h1>Yeni draft</h1><ApiErrorNotice error={error} />
    <ContentForm includeContentType submitLabel="Draft oluştur" onSubmit={async (input) => {
      try { const created = await api.create(input); navigate(`/contents/${created.id}`); } catch (reason) { setError(asApiError(reason)); }
    }} />
  </section>;
}

function ContentDetail({ api, contentId, navigate }: { api: ContentApi; contentId: string; navigate: (path: string) => void }) {
  const [content, setContent] = useState<Content | null>(null);
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isPublishing, setIsPublishing] = useState(false);

  useEffect(() => {
    let isCurrent = true;
    api.get(contentId).then((result) => { if (isCurrent) setContent(result); }).catch((reason) => { if (isCurrent) setError(asApiError(reason)); }).finally(() => { if (isCurrent) setIsLoading(false); });
    return () => { isCurrent = false; };
  }, [api, contentId]);

  if (isLoading) return <p aria-live="polite">İçerik yükleniyor…</p>;
  if (!content) return <section><ApiErrorNotice error={error} /><button onClick={() => navigate("/")}>Listeye dön</button></section>;
  const editable = content.publicationStatus === "DRAFT";

  return <section className="editor">
    <p><a href="/" onClick={(event) => { event.preventDefault(); navigate("/"); }}>← Listeye dön</a></p>
    <div className="title-row"><div><p className="eyebrow">{content.publicationStatus === "DRAFT" ? "Draft" : "Yayınlandı"}</p><h1>{content.title}</h1></div>
      {editable && <button className="publish" disabled={isPublishing} onClick={async () => { setError(null); setIsPublishing(true); try { setContent(await api.publish(content.id)); } catch (reason) { setError(asApiError(reason)); } finally { setIsPublishing(false); } }}>{isPublishing ? "Yayınlanıyor…" : "Yayınla"}</button>}
    </div>
    <ApiErrorNotice error={error} />
    {!editable && <p className="notice">Yayınlanmış içerik değiştirilemez. Bu davranış, yayınlanan katalog bilgisinin sabit kalmasını sağlar.</p>}
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
