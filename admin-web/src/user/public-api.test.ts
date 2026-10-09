import { afterEach, describe, expect, it, vi } from "vitest";
import { PublicApi } from "./public-api";

afterEach(() => vi.unstubAllGlobals());

describe("PublicApi", () => {
  it("reads the published catalogue through the public API contract", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ items: [], page: 0, size: 12, totalItems: 0, totalPages: 0 }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] }).listContents();

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/contents?page=0&size=12", expect.objectContaining({ headers: expect.any(Headers) }));
    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get("X-Test-Actor-Roles")).toBe("USER");
    expect(headers.get("Accept-Language")).toBe("tr");
  });

  it("collects all public content pages for quiz discovery", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ items: [{ id: "content-1" }], page: 0, size: 100, totalItems: 2, totalPages: 2 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ items: [{ id: "content-2" }], page: 1, size: 100, totalItems: 2, totalPages: 2 }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const contents = await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] }).listAllContents();

    expect(contents.map((content) => content.id)).toEqual(["content-1", "content-2"]);
    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/v1/contents?page=0&size=100", expect.anything());
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/v1/contents?page=1&size=100", expect.anything());
  });

  it("sends an answer with the caller-provided idempotency key", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ attemptId: "attempt-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] })
      .submitAnswer("attempt-1", "question-1", "option-1", "answer-key-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/attempts/attempt-1/answers", expect.objectContaining({ method: "POST" }));
    const request = fetchMock.mock.calls[0][1];
    expect((request.headers as Headers).get("Idempotency-Key")).toBe("answer-key-1");
    expect(request.body).toBe(JSON.stringify({ questionId: "question-1", selectedOptionId: "option-1" }));
  });

  it("opens the next question through the server before restarting its timer", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ attemptId: "attempt-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] })
      .startNextQuestion("attempt-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/attempts/attempt-1/next-question",
      expect.objectContaining({ method: "POST" })
    );
  });

  it("loads the quiz discovery list with one public request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] })
      .listPublishedQuizzes();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/quizzes",
      expect.objectContaining({ headers: expect.any(Headers) })
    );
  });

  it("loads the current user's completed quiz XP summaries", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] })
      .listQuizResults();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/me/quiz-results",
      expect.objectContaining({ headers: expect.any(Headers) })
    );
  });

  it("loads protected media with the same viewer identity", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(new Blob(["image"], { type: "image/png" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new PublicApi({ id: "11111111-1111-1111-1111-111111111111", roles: ["USER"] });
    const [media, repeatedMedia] = await Promise.all([
      api.getMedia("/api/v1/media/media-1/content"),
      api.getMedia("/api/v1/media/media-1/content")
    ]);

    expect(media.type).toBe("image/png");
    expect(repeatedMedia).toBe(media);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const request = fetchMock.mock.calls[0][1];
    expect((request.headers as Headers).get("Accept")).toBe("image/*");
    expect((request.headers as Headers).get("X-Test-Actor-Roles")).toBe("USER");
  });
});
