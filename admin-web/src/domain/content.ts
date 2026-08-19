export type ContentType = "SERIES" | "FILM";
export type PublicationStatus = "DRAFT" | "PUBLISHED";
export type WatchLinkFilter = "ALL" | "PRESENT" | "MISSING";

export interface Episode {
  id: string;
  episodeNumber: number;
  title: string;
  description: string | null;
}

export interface Season {
  id: string;
  seasonNumber: number;
  title: string;
  episodes: Episode[];
}

export interface Content {
  id: string;
  title: string;
  description: string | null;
  watchUrl: string | null;
  coverMediaId: string | null;
  coverImageUrl: string | null;
  coverAlternativeText: string | null;
  contentType: ContentType;
  publicationStatus: PublicationStatus;
  createdAt: string;
  updatedAt: string;
  seasons: Season[];
}

export interface ContentSummary {
  id: string;
  title: string;
  description: string | null;
  coverMediaId: string | null;
  coverImageUrl: string | null;
  coverAlternativeText: string | null;
  contentType: ContentType;
  publicationStatus: PublicationStatus;
  hasWatchUrl: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ContentPage {
  items: ContentSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ContentInput {
  title: string;
  description: string;
  contentType: ContentType;
}

export interface CoverBindingInput {
  mediaAssetId: string;
  alternativeText: string;
}

export interface SeasonInput {
  seasonNumber: number;
  title: string;
}

export interface SeasonPlanInput {
  seasons: Array<{
    seasonNumber: number;
    episodeCount: number;
  }>;
}

export interface EpisodeInput {
  episodeNumber: number;
  title: string;
  description: string;
}

export interface ContentTranslation {
  title: string;
  description: string | null;
  coverAlternativeText: string | null;
  seasons: Array<{ seasonId: string; title: string; episodes: Array<{ episodeId: string; title: string; description: string | null }> }>;
}
