import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./client";
import { QuizApi } from "./quiz-api";

describe("QuizApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("creates an episode-scoped quiz with structured references", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      id: "quiz-1", contentId: "content-1", scopeType: "EPISODE",
      seasonId: "season-1", episodeId: "episode-1", versions: []
    }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new QuizApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.create({ contentId: "content-1", scopeType: "EPISODE", seasonId: "season-1", episodeId: "episode-1", title: "Bölüm Quizi", description: "" });

    const [path, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe("/api/v1/admin/quizzes");
    expect(JSON.parse(request.body as string)).toMatchObject({ scopeType: "EPISODE", seasonId: "season-1", episodeId: "episode-1" });
    expect((request.headers as Headers).get("X-Test-Actor-Roles")).toBe("EDITOR");
  });

  it("keeps the correct answer only in the admin question request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "quiz-1", versions: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new QuizApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.addQuestion("quiz-1", "version-1", {
      questionOrder: 1, prompt: "Kimdir?",
      visualMediaId: null, visualRole: null, visualAlternativeText: null,
      accessiblePrompt: null,
      answerOptions: [{ optionOrder: 1, text: "A", correct: true }, { optionOrder: 2, text: "B", correct: false }, { optionOrder: 3, text: "C", correct: false }, { optionOrder: 4, text: "D", correct: false }]
    });

    const request = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(request.body as string).answerOptions).toEqual([
      { optionOrder: 1, text: "A", correct: true },
      { optionOrder: 2, text: "B", correct: false },
      { optionOrder: 3, text: "C", correct: false },
      { optionOrder: 4, text: "D", correct: false }
    ]);
  });

  it("permanently deletes a quiz", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new QuizApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.deleteQuiz("quiz-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/quizzes/quiz-1",
      expect.objectContaining({ method: "DELETE" })
    );
  });

  it("moves a published quiz to history", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "quiz-1", versions: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new QuizApi(new ApiClient({ id: "editor-1", roles: ["EDITOR"] }));

    await api.retireQuiz("quiz-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/admin/quizzes/quiz-1/retire",
      expect.objectContaining({ method: "POST" })
    );
  });
});
