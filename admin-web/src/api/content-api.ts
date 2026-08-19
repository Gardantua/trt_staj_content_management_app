import { ApiClient } from "./client";
import type { Content, ContentInput, ContentPage, ContentTranslation, CoverBindingInput, EpisodeInput, SeasonInput, SeasonPlanInput } from "../domain/content";

export class ContentApi {
  constructor(private readonly client: ApiClient) {}

  list(page: number, size: number, query = ""): Promise<ContentPage> {
    const parameters = new URLSearchParams({ page: String(page), size: String(size) });
    if (query.trim()) parameters.set("query", query.trim());
    return this.client.request(`/api/v1/admin/contents?${parameters.toString()}`);
  }

  get(contentId: string): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}`);
  }

  getTranslation(contentId: string): Promise<ContentTranslation> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/translations/en`);
  }

  saveTranslation(contentId: string, input: ContentTranslation): Promise<ContentTranslation> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/translations/en`, { method: "PUT", body: JSON.stringify(input) });
  }

  create(input: ContentInput): Promise<Content> {
    return this.client.request("/api/v1/admin/contents", { method: "POST", body: JSON.stringify(input) });
  }

  update(contentId: string, input: Pick<ContentInput, "title" | "description">): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}`, { method: "PUT", body: JSON.stringify(input) });
  }

  setWatchUrl(contentId: string, watchUrl: string): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/watch-url`, { method: "PUT", body: JSON.stringify({ watchUrl }) });
  }

  publish(contentId: string): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/publish`, { method: "POST" });
  }

  deleteContent(contentId: string): Promise<void> {
    return this.client.request(`/api/v1/admin/contents/${contentId}`, { method: "DELETE" });
  }

  setCover(contentId: string, input: CoverBindingInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/cover`, { method: "PUT", body: JSON.stringify(input) });
  }

  addSeason(contentId: string, input: SeasonInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons`, { method: "POST", body: JSON.stringify(input) });
  }

  addSeasonPlan(contentId: string, input: SeasonPlanInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/season-plan`, { method: "POST", body: JSON.stringify(input) });
  }

  updateSeason(contentId: string, seasonId: string, input: SeasonInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons/${seasonId}`, { method: "PUT", body: JSON.stringify(input) });
  }

  deleteSeason(contentId: string, seasonId: string): Promise<void> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons/${seasonId}`, { method: "DELETE" });
  }

  addEpisode(contentId: string, seasonId: string, input: EpisodeInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons/${seasonId}/episodes`, { method: "POST", body: JSON.stringify(input) });
  }

  updateEpisode(contentId: string, seasonId: string, episodeId: string, input: EpisodeInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons/${seasonId}/episodes/${episodeId}`, { method: "PUT", body: JSON.stringify(input) });
  }

  deleteEpisode(contentId: string, seasonId: string, episodeId: string): Promise<void> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons/${seasonId}/episodes/${episodeId}`, { method: "DELETE" });
  }
}
