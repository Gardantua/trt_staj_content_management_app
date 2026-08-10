import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./client";
import { MediaApi } from "./media-api";

describe("MediaApi", () => {
  afterEach(() => vi.unstubAllGlobals());

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
});
