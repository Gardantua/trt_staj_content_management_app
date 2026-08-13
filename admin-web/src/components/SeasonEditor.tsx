import { useEffect, useState, type FormEvent } from "react";
import type { Content, Episode, EpisodeInput, Season, SeasonInput, SeasonPlanInput } from "../domain/content";

interface SeasonEditorProps {
  content: Content;
  onContentChanged: (content: Content) => void;
  onError: (reason: unknown) => void;
  onAddSeasonPlan: (input: SeasonPlanInput) => Promise<Content>;
  onAddEpisode: (season: Season, input: EpisodeInput) => Promise<Content>;
  onUpdateSeason: (season: Season, input: SeasonInput) => Promise<Content>;
  onDeleteSeason: (season: Season) => Promise<void>;
  onUpdateEpisode: (season: Season, episode: Episode, input: EpisodeInput) => Promise<Content>;
  onDeleteEpisode: (season: Season, episode: Episode) => Promise<void>;
}

interface PlannedSeason {
  seasonNumber: number;
  episodeCount: number;
}

export function SeasonEditor(props: SeasonEditorProps) {
  const { content, onContentChanged } = props;
  const canModifyExisting = content.publicationStatus === "DRAFT";
  const firstSeasonNumber = nextSeasonNumber(content);
  const [seasonCount, setSeasonCount] = useState(1);
  const [plannedSeasons, setPlannedSeasons] = useState<PlannedSeason[]>(() => createPlans(firstSeasonNumber, 1));
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setSeasonCount(1);
    setPlannedSeasons(createPlans(firstSeasonNumber, 1));
  }, [firstSeasonNumber]);

  function changeSeasonCount(nextCount: number) {
    const boundedCount = Math.min(100, Math.max(1, nextCount));
    setSeasonCount(boundedCount);
    setPlannedSeasons((current) => createPlans(firstSeasonNumber, boundedCount, current));
  }

  async function submitPlan(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const changedContent = await props.onAddSeasonPlan({ seasons: plannedSeasons });
      onContentChanged(changedContent);
    } catch (reason) {
      props.onError(reason);
    } finally {
      setIsSubmitting(false);
    }
  }

  return <section aria-labelledby="seasons-heading" className="season-editor">
    <div className="season-editor__heading">
      <h2 id="seasons-heading">Sezonlar ve bölümler</h2>
      <p>{canModifyExisting ? "Sezonları ve her sezondaki bölüm sayısını birlikte girin. Bölüm adları otomatik oluşturulur." : "Yeni sezon ve bölümler ekleyebilirsiniz. Yayımlanmış mevcut kayıtlar değiştirilemez veya silinemez."}</p>
    </div>
    <form className="season-plan-form" onSubmit={submitPlan}>
      <div className="season-plan-form__count">
        <label>Eklenecek sezon sayısı<input type="number" min={1} max={100} required value={seasonCount} onChange={(event) => changeSeasonCount(Number(event.target.value))} /></label>
        <p className="field-help">Yeni sezonlar {firstSeasonNumber}. sezondan başlayarak sırayla numaralandırılır.</p>
      </div>
      <div className="season-plan-grid">
        {plannedSeasons.map((plannedSeason, index) => <article key={plannedSeason.seasonNumber}>
          <h3>{plannedSeason.seasonNumber}. Sezon</h3>
          <label>Bölüm sayısı<input type="number" min={1} max={1000} required value={plannedSeason.episodeCount} onChange={(event) => setPlannedSeasons((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, episodeCount: Number(event.target.value) } : item))} /></label>
          <p>1. Bölüm–{plannedSeason.episodeCount}. Bölüm otomatik oluşur.</p>
        </article>)}
      </div>
      <button className="button-primary" disabled={isSubmitting}>{isSubmitting ? "Oluşturuluyor…" : "Sezonları ve bölümleri oluştur"}</button>
    </form>
    {content.seasons.map((season) => <SeasonCard key={season.id} season={season} canModifyExisting={canModifyExisting} onContentChanged={onContentChanged} onError={props.onError} onAddEpisode={props.onAddEpisode} onUpdateSeason={props.onUpdateSeason} onDeleteSeason={props.onDeleteSeason} onUpdateEpisode={props.onUpdateEpisode} onDeleteEpisode={props.onDeleteEpisode} />)}
  </section>;
}

function SeasonCard({ season, canModifyExisting, onContentChanged, onError, onAddEpisode, onUpdateSeason, onDeleteSeason, onUpdateEpisode, onDeleteEpisode }: Pick<SeasonEditorProps, "onContentChanged" | "onError" | "onAddEpisode" | "onUpdateSeason" | "onDeleteSeason" | "onUpdateEpisode" | "onDeleteEpisode"> & { season: Season; canModifyExisting: boolean }) {
  const [number, setNumber] = useState(season.seasonNumber);
  const [title, setTitle] = useState(season.title);
  const [showEpisodes, setShowEpisodes] = useState(false);

  return <article className="nested-card">
    {canModifyExisting ? <form className="inline-form" onSubmit={async (event) => { event.preventDefault(); try { onContentChanged(await onUpdateSeason(season, { seasonNumber: number, title: title.trim() })); } catch (reason) { onError(reason); } }}>
      <h3>{season.seasonNumber}. sezon · {season.episodes.length} bölüm</h3>
      <label>Numara<input type="number" min={1} max={10000} required value={number} onChange={(event) => setNumber(Number(event.target.value))} /></label>
      <label>Başlık<input maxLength={200} required value={title} onChange={(event) => setTitle(event.target.value)} /></label>
      <button>Sezonu kaydet</button>
      <button type="button" onClick={() => setShowEpisodes((visible) => !visible)}>{showEpisodes ? "Bölümleri kapat" : "Bölümleri düzenle"}</button>
      <button type="button" className="danger" onClick={async () => { if (window.confirm(`${season.title} sezonunu ve bölümlerini silmek istiyor musunuz?`)) { try { await onDeleteSeason(season); } catch (reason) { onError(reason); } } }}>Sezonu sil</button>
    </form> : <div className="inline-form"><h3>{season.seasonNumber}. sezon · {season.episodes.length} bölüm</h3><button type="button" onClick={() => setShowEpisodes((visible) => !visible)}>{showEpisodes ? "Bölümleri kapat" : "Bölümleri göster"}</button></div>}
    <EpisodeAddForm season={season} onContentChanged={onContentChanged} onError={onError} onAddEpisode={onAddEpisode} />
    {showEpisodes ? <ul className="episode-list">{season.episodes.map((episode) => canModifyExisting ? <EpisodeRow key={episode.id} episode={episode} season={season} onContentChanged={onContentChanged} onError={onError} onUpdateEpisode={onUpdateEpisode} onDeleteEpisode={onDeleteEpisode} /> : <li key={episode.id}><strong>{episode.episodeNumber}. Bölüm · {episode.title}</strong>{episode.description ? <p>{episode.description}</p> : null}</li>)}</ul> : null}
  </article>;
}

function EpisodeAddForm({ season, onContentChanged, onError, onAddEpisode }: Pick<SeasonEditorProps, "onContentChanged" | "onError" | "onAddEpisode"> & { season: Season }) {
  const episodeNumber = nextEpisodeNumber(season);
  const [title, setTitle] = useState(`${episodeNumber}. Bölüm`);
  const [description, setDescription] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => setTitle(`${episodeNumber}. Bölüm`), [episodeNumber]);

  return <form className="inline-form" onSubmit={async (event) => { event.preventDefault(); setIsSubmitting(true); try { onContentChanged(await onAddEpisode(season, { episodeNumber, title: title.trim(), description: description.trim() })); setDescription(""); } catch (reason) { onError(reason); } finally { setIsSubmitting(false); } }}>
    <strong>Yeni bölüm ekle</strong>
    <label>Numara<input value={episodeNumber} readOnly /></label>
    <label>Başlık<input maxLength={200} required value={title} onChange={(event) => setTitle(event.target.value)} /></label>
    <label>Açıklama<input maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
    <button className="button-secondary" disabled={isSubmitting}>{isSubmitting ? "Ekleniyor…" : "Bölümü ekle"}</button>
  </form>;
}

function EpisodeRow({ episode, season, onContentChanged, onError, onUpdateEpisode, onDeleteEpisode }: { episode: Episode; season: Season; onContentChanged: (content: Content) => void; onError: SeasonEditorProps["onError"]; onUpdateEpisode: SeasonEditorProps["onUpdateEpisode"]; onDeleteEpisode: SeasonEditorProps["onDeleteEpisode"] }) {
  const [number, setNumber] = useState(episode.episodeNumber);
  const [title, setTitle] = useState(episode.title);
  const [description, setDescription] = useState(episode.description ?? "");
  return <li><form className="inline-form" onSubmit={async (event) => { event.preventDefault(); try { onContentChanged(await onUpdateEpisode(season, episode, { episodeNumber: number, title: title.trim(), description: description.trim() })); } catch (reason) { onError(reason); } }}>
    <label>Bölüm numarası<input type="number" min={1} max={100000} required value={number} onChange={(event) => setNumber(Number(event.target.value))} /></label>
    <label>Başlık<input maxLength={200} required value={title} onChange={(event) => setTitle(event.target.value)} /></label>
    <label>Açıklama<input maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
    <button>Bölümü kaydet</button>
    <button type="button" className="danger" onClick={async () => { if (window.confirm(`${episode.title} bölümünü silmek istiyor musunuz?`)) { try { await onDeleteEpisode(season, episode); } catch (reason) { onError(reason); } } }}>Bölümü sil</button>
  </form></li>;
}

function createPlans(firstSeasonNumber: number, count: number, current: PlannedSeason[] = []): PlannedSeason[] {
  return Array.from({ length: count }, (_, index) => ({
    seasonNumber: firstSeasonNumber + index,
    episodeCount: current[index]?.episodeCount ?? 1
  }));
}

function nextSeasonNumber(content: Content): number {
  return Math.max(0, ...content.seasons.map((season) => season.seasonNumber)) + 1;
}

export function nextEpisodeNumber(season: Season): number {
  return Math.max(0, ...season.episodes.map((episode) => episode.episodeNumber)) + 1;
}
