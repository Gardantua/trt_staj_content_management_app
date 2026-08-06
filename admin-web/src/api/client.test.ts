import { describe, expect, it } from "vitest";
import { readApiError } from "./client";

describe("readApiError", () => {
  it("preserves backend error code and trace identifier for support", async () => {
    const error = await readApiError(new Response(JSON.stringify({ code: "CONTENT_NOT_PUBLISHABLE", message: "Kapak gerekli.", traceId: "trace-123" }), { status: 409 }));
    expect(error).toMatchObject({ code: "CONTENT_NOT_PUBLISHABLE", message: "Kapak gerekli.", traceId: "trace-123", status: 409 });
  });

  it("uses safe fallback details for non-JSON error responses", async () => {
    const error = await readApiError(new Response("bad gateway", { status: 502 }));
    expect(error).toMatchObject({ code: "UNEXPECTED_API_ERROR", traceId: "unavailable", status: 502 });
  });
});
