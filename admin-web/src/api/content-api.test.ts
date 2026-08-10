import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./client";
import { ContentApi } from "./content-api";

describe("ContentApi", () => {
  afterEach(() => vi.unstubAllGlobals());

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
});
