import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthApi } from "./auth-api";

afterEach(() => vi.unstubAllGlobals());
beforeEach(() => vi.stubGlobal("document", { cookie: "XSRF-TOKEN=csrf-token" }));

describe("AuthApi", () => {
  it("registers a USER account through the session API", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(JSON.stringify({ actorId: "user-1", roles: ["USER"] }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await new AuthApi().register("yunus@example.com", "Yunus", "secret123");

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/v1/auth/register", expect.objectContaining({
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify({ email: "yunus@example.com", displayName: "Yunus", password: "secret123" })
    }));
  });

  it("restores and closes the cookie session", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ actorId: "user-1", roles: ["USER"] }), { status: 200 }))
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new AuthApi();
    await api.me();
    await api.logout();

    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/v1/auth/me", expect.objectContaining({ credentials: "same-origin" }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/v1/auth/logout", expect.objectContaining({ method: "POST" }));
    const logoutHeaders = fetchMock.mock.calls[2][1].headers as Headers;
    expect(logoutHeaders.get("X-XSRF-TOKEN")).toBe("csrf-token");
  });

  it("requests and completes a password reset without exposing the token elsewhere", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 202 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new AuthApi();
    await api.requestPasswordReset("yunus@example.com");
    await api.resetPassword("single-use-token", "new-secret-123");

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/v1/auth/password-reset/request", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ email: "yunus@example.com" })
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/v1/auth/password-reset/reset", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ token: "single-use-token", newPassword: "new-secret-123" })
    }));
  });
});

function csrfResponse() {
  return new Response(JSON.stringify({ headerName: "X-XSRF-TOKEN", token: "csrf-token" }), { status: 200 });
}
