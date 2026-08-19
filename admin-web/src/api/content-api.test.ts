import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./client";
import { ContentApi } from "./content-api";

describe("ContentApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("sends a trimmed and encoded title query with admin pagination", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ items: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.list(1, 20, "  Diriliş Ertuğrul  ");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents?page=1&size=20&query=Dirili%C5%9F+Ertu%C4%9Frul",
      expect.objectContaining({ headers: expect.any(Headers) })
    );
  });

  it("binds an uploaded media identifier and alternative text to the content cover", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "content-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.setCover("content-1", { mediaAssetId: "media-1", alternativeText: "Bir karakter sahilde yürüyor" });

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/admin/contents/content-1/cover", expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({ mediaAssetId: "media-1", alternativeText: "Bir karakter sahilde yürüyor" })
    }));
  });

  it("saves the official watch URL through the protected content endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "content-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));
    const watchUrl = "https://www.tabii.com/tr/detail/115660/ibi";

    await api.setWatchUrl("content-1", watchUrl);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents/content-1/watch-url",
      expect.objectContaining({ method: "PUT", body: JSON.stringify({ watchUrl }) })
    );
  });

  it("adds the selected watch-link filter to the admin catalog request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ items: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.list(0, 20, "", "MISSING");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents?page=0&size=20&watchLink=MISSING",
      expect.objectContaining({ headers: expect.any(Headers) })
    );
  });

  it("hard deletes content through the protected admin endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.deleteContent("content-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents/content-1",
      expect.objectContaining({ method: "DELETE" })
    );
  });

  it("creates a season and episode plan with one request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "content-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));
    const plan = { seasons: [{ seasonNumber: 1, episodeCount: 29 }, { seasonNumber: 2, episodeCount: 30 }] };

    await api.addSeasonPlan("content-1", plan);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents/content-1/season-plan",
      expect.objectContaining({ method: "POST", body: JSON.stringify(plan) })
    );
  });

  it("appends an episode through the protected content hierarchy endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "content-1" }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new ContentApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));
    const episode = { episodeNumber: 8, title: "8. Bölüm", description: "" };

    await api.addEpisode("content-1", "season-1", episode);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/contents/content-1/seasons/season-1/episodes",
      expect.objectContaining({ method: "POST", body: JSON.stringify(episode) })
    );
  });
});
