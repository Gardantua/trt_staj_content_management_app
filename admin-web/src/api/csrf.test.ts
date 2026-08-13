import { afterEach, describe, expect, it, vi } from "vitest";
import { CsrfTokenClient } from "./csrf";

afterEach(() => vi.unstubAllGlobals());

describe("CsrfTokenClient", () => {
  it("loads one token and adds it only to unsafe requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      headerName: "X-XSRF-TOKEN",
      token: "token-1"
    }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    vi.stubGlobal("document", { cookie: "XSRF-TOKEN=token-1" });
    const client = new CsrfTokenClient("");
    const getHeaders = new Headers();
    const postHeaders = new Headers();

    await client.protect(getHeaders, "GET");
    await client.protect(postHeaders, "POST");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(postHeaders.get("X-XSRF-TOKEN")).toBe("token-1");
    expect(getHeaders.has("X-XSRF-TOKEN")).toBe(false);
  });
});
