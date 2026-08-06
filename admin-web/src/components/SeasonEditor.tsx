import { useState, type FormEvent } from "react";
import type { Content, Episode, EpisodeInput, Season, SeasonInput } from "../domain/content";

interface SeasonEditorProps {
  content: Content;
  onContentChanged: (content: Content) => void;
  onError: (reason: unknown) => void;
  onAddSeason: (input: SeasonInput) => Promise<Content>;
  onUpdateSeason: (season: Season, input: SeasonInput) => Promise<Content>;
  onDeleteSeason: (season: Season) => Promise<void>;
  onAddEpisode: (season: Season, input: EpisodeInput) => Promise<Content>;
  onUpdateEpisode: (season: Season, episode: Episode, input: EpisodeInput) => Promise<Content>;
  onDeleteEpisode: (season: Season, episode: Episode) => Promise<void>;
}

export function SeasonEditor(props: SeasonEditorProps) {
  const { content, onContentChanged } = props;
  const [seasonNumber, setSeasonNumber] = useState(nextSeasonNumber(content));
  const [seasonTitle, setSeasonTitle] = useState("");
  const [isAddingSeason, setIsAddingSeason] = useState(false);

  async function addSeason(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsAddingSeason(true);
    try {
      const changedContent = await props.onAddSeason({ seasonNumber, title: seasonTitle.trim() });
      onContentChanged(changedContent);
      setSeasonNumber(nextSeasonNumber(changedContent));
      setSeasonTitle("");
    } catch (reason) { props.onError(reason); } finally { setIsAddingSeason(false); }
  }

  return (
    <section aria-labelledby="seasons-heading">
      <h2 id="seasons-heading">Sezonlar ve bölümler</h2>
      {content.seasons.map((season) => (
        <SeasonCard key={season.id} season={season} onContentChanged={onContentChanged} onError={props.onError} onUpdateSeason={props.onUpdateSeason} onDeleteSeason={props.onDeleteSeason} onAddEpisode={props.onAddEpisode} onUpdateEpisode={props.onUpdateEpisode} onDeleteEpisode={props.onDeleteEpisode} />
      ))}
      <form className="inline-form" onSubmit={addSeason}>
        <h3>Sezon ekle</h3>
        <label>Sezon numarası<input type="number" min={1} max={10000} required value={seasonNumber} onChange={(e) => setSeasonNumber(Number(e.target.value))} /></label>
        <label>Başlık<input required maxLength={200} value={seasonTitle} onChange={(e) => setSeasonTitle(e.target.value)} /></label>
        <button disabled={isAddingSeason}>{isAddingSeason ? "Ekleniyor…" : "Sezon ekle"}</button>
      </form>
    </section>
  );
}

function SeasonCard({ season, onContentChanged, onError, onUpdateSeason, onDeleteSeason, onAddEpisode, onUpdateEpisode, onDeleteEpisode }: Pick<SeasonEditorProps, "onContentChanged" | "onError" | "onUpdateSeason" | "onDeleteSeason" | "onAddEpisode" | "onUpdateEpisode" | "onDeleteEpisode"> & { season: Season }) {
  const [number, setNumber] = useState(season.seasonNumber);
  const [title, setTitle] = useState(season.title);
  const [episodeNumber, setEpisodeNumber] = useState(nextEpisodeNumber(season));
  const [episodeTitle, setEpisodeTitle] = useState("");
  const [episodeDescription, setEpisodeDescription] = useState("");

  async function addEpisode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const changedContent = await onAddEpisode(season, {
        episodeNumber,
        title: episodeTitle.trim(),
        description: episodeDescription.trim()
      });
      onContentChanged(changedContent);
      const changedSeason = changedContent.seasons.find((candidate) => candidate.id === season.id);
      setEpisodeNumber(changedSeason ? nextEpisodeNumber(changedSeason) : episodeNumber + 1);
      setEpisodeTitle("");
      setEpisodeDescription("");
    } catch (reason) {
      onError(reason);
    }
  }

  return <article className="nested-card">
    <form className="inline-form" onSubmit={async (event) => { event.preventDefault(); try { onContentChanged(await onUpdateSeason(season, { seasonNumber: number, title: title.trim() })); } catch (reason) { onError(reason); } }}>
      <h3>Sezon</h3>
      <label>Numara<input type="number" min={1} max={10000} required value={number} onChange={(e) => setNumber(Number(e.target.value))} /></label>
      <label>Başlık<input maxLength={200} required value={title} onChange={(e) => setTitle(e.target.value)} /></label>
      <button>Sezonu kaydet</button>
      <button type="button" className="danger" onClick={async () => { if (window.confirm(`${season.title} sezonunu ve bölümlerini silmek istiyor musunuz?`)) { try { await onDeleteSeason(season); } catch (reason) { onError(reason); } } }}>Sezonu sil</button>
    </form>
    <ul className="episode-list">
      {season.episodes.map((episode) => <EpisodeRow key={episode.id} episode={episode} season={season} onContentChanged={onContentChanged} onError={onError} onUpdateEpisode={onUpdateEpisode} onDeleteEpisode={onDeleteEpisode} />)}
    </ul>
    <form className="inline-form" onSubmit={addEpisode}>
      <h4>Bölüm ekle</h4>
      <label>Numara<input type="number" min={1} max={100000} required value={episodeNumber} onChange={(e) => setEpisodeNumber(Number(e.target.value))} /></label>
      <label>Başlık<input maxLength={200} required value={episodeTitle} onChange={(e) => setEpisodeTitle(e.target.value)} /></label>
      <label>Açıklama<input maxLength={2000} value={episodeDescription} onChange={(e) => setEpisodeDescription(e.target.value)} /></label>
      <button>Bölüm ekle</button>
    </form>
  </article>;
}

function EpisodeRow({ episode, season, onContentChanged, onError, onUpdateEpisode, onDeleteEpisode }: { episode: Episode; season: Season; onContentChanged: (content: Content) => void; onError: SeasonEditorProps["onError"]; onUpdateEpisode: SeasonEditorProps["onUpdateEpisode"]; onDeleteEpisode: SeasonEditorProps["onDeleteEpisode"] }) {
  const [number, setNumber] = useState(episode.episodeNumber);
  const [title, setTitle] = useState(episode.title);
  const [description, setDescription] = useState(episode.description ?? "");
  return <li><form className="inline-form" onSubmit={async (event) => { event.preventDefault(); try { onContentChanged(await onUpdateEpisode(season, episode, { episodeNumber: number, title: title.trim(), description: description.trim() })); } catch (reason) { onError(reason); } }}>
    <label>Bölüm numarası<input type="number" min={1} max={100000} required value={number} onChange={(e) => setNumber(Number(e.target.value))} /></label>
    <label>Başlık<input maxLength={200} required value={title} onChange={(e) => setTitle(e.target.value)} /></label>
    <label>Açıklama<input maxLength={2000} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
    <button>Bölümü kaydet</button>
    <button type="button" className="danger" onClick={async () => { if (window.confirm(`${episode.title} bölümünü silmek istiyor musunuz?`)) { try { await onDeleteEpisode(season, episode); } catch (reason) { onError(reason); } } }}>Bölümü sil</button>
  </form></li>;
}

function nextSeasonNumber(content: Content): number { return Math.max(0, ...content.seasons.map((season) => season.seasonNumber)) + 1; }
function nextEpisodeNumber(season: Season): number { return Math.max(0, ...season.episodes.map((episode) => episode.episodeNumber)) + 1; }
