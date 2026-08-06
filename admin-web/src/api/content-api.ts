import { ApiClient } from "./client";
import type { Content, ContentDraftInput, ContentPage, EpisodeInput, SeasonInput } from "../domain/content";

export class ContentApi {
  constructor(private readonly client: ApiClient) {}

  list(page: number, size: number): Promise<ContentPage> {
    return this.client.request(`/api/v1/admin/contents?page=${page}&size=${size}`);
  }

  get(contentId: string): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}`);
  }

  create(input: ContentDraftInput): Promise<Content> {
    return this.client.request("/api/v1/admin/contents", { method: "POST", body: JSON.stringify(input) });
  }

  update(contentId: string, input: Pick<ContentDraftInput, "title" | "description">): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}`, { method: "PUT", body: JSON.stringify(input) });
  }

  publish(contentId: string): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/publish`, { method: "POST" });
  }

  addSeason(contentId: string, input: SeasonInput): Promise<Content> {
    return this.client.request(`/api/v1/admin/contents/${contentId}/seasons`, { method: "POST", body: JSON.stringify(input) });
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
