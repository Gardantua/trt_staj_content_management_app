import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./client";
import { MediaApi } from "./media-api";

describe("MediaApi", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("sends the image as multipart data without overriding its boundary", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      id: "media-1", mediaType: "IMAGE", mimeType: "image/png", byteSize: 12, width: 10, height: 10, contentUrl: "/api/v1/media/media-1/content"
    }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new MediaApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));
    const file = new Blob(["cover"], { type: "image/png" }) as File;

    const result = await api.uploadImage(file);

    expect(result.id).toBe("media-1");
    expect(fetchMock).toHaveBeenCalledOnce();
    const [path, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe("/api/v1/admin/media/images");
    expect(request.method).toBe("POST");
    expect(request.headers).toBeInstanceOf(Headers);
    expect((request.headers as Headers).get("Content-Type")).toBeNull();
    expect((request.headers as Headers).get("X-Test-Actor-Roles")).toBe("EDITOR");
    expect(request.body).toBeInstanceOf(FormData);
    expect((request.body as FormData).get("file")).toBeInstanceOf(Blob);
  });

  it("downloads the same protected image once during the admin session", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(new Blob(["image"], { type: "image/png" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(URL, "createObjectURL").mockReturnValueOnce("blob:first").mockReturnValueOnce("blob:second");
    const api = new MediaApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    const urls = await Promise.all([
      api.loadImageObjectUrl("/api/v1/media/media-1/content"),
      api.loadImageObjectUrl("/api/v1/media/media-1/content")
    ]);

    expect(urls).toEqual(["blob:first", "blob:second"]);
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it("does not cache a failed image request so refresh can retry it", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError("network down"))
      .mockResolvedValueOnce(new Response(new Blob(["image"], { type: "image/png" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:retry");
    const api = new MediaApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await expect(api.loadImageObjectUrl("/api/v1/media/media-1/content")).rejects.toMatchObject({ code: "NETWORK_UNAVAILABLE" });
    await expect(api.loadImageObjectUrl("/api/v1/media/media-1/content")).resolves.toBe("blob:retry");
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
