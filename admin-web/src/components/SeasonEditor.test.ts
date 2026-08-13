import { describe, expect, it } from "vitest";
import { nextEpisodeNumber } from "./SeasonEditor";

describe("SeasonEditor", () => {
  it("places a new episode after the current published hierarchy", () => {
    expect(nextEpisodeNumber({
      id: "season-1",
      seasonNumber: 1,
      title: "1. Sezon",
      episodes: [
        { id: "episode-1", episodeNumber: 1, title: "1. Bölüm", description: null },
        { id: "episode-3", episodeNumber: 3, title: "3. Bölüm", description: null }
      ]
    })).toBe(4);
  });
});
