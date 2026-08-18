import type { LocalActor } from "../auth/actor";
import { CsrfTokenClient } from "../api/csrf";
import { readStoredLanguage, translate } from "../i18n/I18nContext";
import type { AnswerSubmissionResult, CurrentActor, Leaderboard, PublicApiError, PublicContent, PublicContentPage, PublishedQuiz, PublishedQuizSummary, QuizAttempt, QuizResultSummary, XpSummary } from "./types";

export class PublicApiRequestError extends Error implements PublicApiError {
  readonly code: string;
  readonly traceId: string;
  readonly status: number;

  constructor(error: PublicApiError) {
    super(error.message);
    this.name = "PublicApiRequestError";
    this.code = error.code;
    this.traceId = error.traceId;
    this.status = error.status;
  }
}

interface ErrorBody { code?: unknown; message?: unknown; traceId?: unknown; }

export async function toApiError(response: Response): Promise<PublicApiRequestError> {
  let body: ErrorBody = {};
  try { body = await response.json() as ErrorBody; } catch { /* A proxy error can be plain text. */ }
  return new PublicApiRequestError({
    code: typeof body.code === "string" ? body.code : "UNEXPECTED_API_ERROR",
    message: typeof body.message === "string" ? body.message : translate(readStoredLanguage(), "error.requestFailed"),
    traceId: typeof body.traceId === "string" ? body.traceId : "unavailable",
    status: response.status
  });
}

export class PublicApi {
  private readonly mediaRequests = new Map<string, Promise<Blob>>();
  private readonly csrfTokenClient: CsrfTokenClient;

  constructor(
    private readonly actor: LocalActor | null = null,
    private readonly baseUrl = import.meta.env.VITE_API_BASE_URL ?? ""
  ) {
    this.csrfTokenClient = new CsrfTokenClient(baseUrl);
  }

  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers({ Accept: "application/json" });
    new Headers(init.headers).forEach((value, name) => headers.set(name, value));
    if (init.body !== undefined) headers.set("Content-Type", "application/json");
    if (this.actor?.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    if (!this.actor?.id) await this.csrfTokenClient.protect(headers, init.method);
    let response: Response;
    try { response = await fetch(`${this.baseUrl}${path}`, { ...init, headers, credentials: "same-origin" }); }
    catch {
      throw new PublicApiRequestError({ code: "NETWORK_UNAVAILABLE", message: translate(readStoredLanguage(), "error.contentUnavailable"), traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await toApiError(response);
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }

  get<T>(path: string): Promise<T> { return this.request(path); }

  listContents(page = 0, size = 12): Promise<PublicContentPage> {
    return this.get(`/api/v1/contents?page=${page}&size=${size}`);
  }

  async listAllContents(): Promise<PublicContentPage["items"]> {
    const contents: PublicContentPage["items"] = [];
    let currentPage = 0;
    let totalPages = 1;
    while (currentPage < totalPages) {
      const page = await this.listContents(currentPage, 100);
      contents.push(...page.items);
      totalPages = page.totalPages;
      currentPage += 1;
    }
    return contents;
  }

  getContent(contentId: string): Promise<PublicContent> {
    return this.get(`/api/v1/contents/${contentId}`);
  }

  listQuizzes(contentId: string): Promise<PublishedQuiz[]> {
    return this.get(`/api/v1/contents/${contentId}/quizzes`);
  }

  listPublishedQuizzes(): Promise<PublishedQuizSummary[]> {
    return this.get("/api/v1/quizzes");
  }
  listQuizResults(): Promise<QuizResultSummary[]> {
    return this.get("/api/v1/me/quiz-results");
  }

  getQuiz(quizId: string): Promise<PublishedQuiz> { return this.get(`/api/v1/quizzes/${quizId}`); }
  startAttempt(quizId: string): Promise<QuizAttempt> {
    return this.request(`/api/v1/quizzes/${quizId}/attempts`, { method: "POST" });
  }
  submitAnswer(attemptId: string, questionId: string, selectedOptionId: string, idempotencyKey: string): Promise<AnswerSubmissionResult> {
    return this.request(`/api/v1/attempts/${attemptId}/answers`, { method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body: JSON.stringify({ questionId, selectedOptionId }) });
  }
  timeoutQuestion(attemptId: string, questionId: string, idempotencyKey: string): Promise<QuizAttempt> {
    return this.request(`/api/v1/attempts/${attemptId}/timeouts`, { method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body: JSON.stringify({ questionId }) });
  }
  startNextQuestion(attemptId: string): Promise<QuizAttempt> {
    return this.request(`/api/v1/attempts/${attemptId}/next-question`, { method: "POST" });
  }
  abandonAttempt(attemptId: string): Promise<QuizAttempt> {
    return this.request(`/api/v1/attempts/${attemptId}/abandon`, { method: "POST" });
  }
  getXp(): Promise<XpSummary> { return this.get("/api/v1/me/xp"); }
  getIdentity(): Promise<CurrentActor> { return this.get("/api/v1/identity/me"); }
  getGlobalLeaderboard(): Promise<Leaderboard> { return this.get("/api/v1/leaderboards/global?limit=10"); }

  getMedia(path: string): Promise<Blob> {
    const cachedRequest = this.mediaRequests.get(path);
    if (cachedRequest) return cachedRequest;
    const request = this.loadMedia(path).catch((error: unknown) => { this.mediaRequests.delete(path); throw error; });
    this.mediaRequests.set(path, request);
    return request;
  }

  private async loadMedia(path: string): Promise<Blob> {
    const headers = new Headers({ Accept: "image/*" });
    if (this.actor?.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    let response: Response;
    try { response = await fetch(`${this.baseUrl}${path}`, { headers, credentials: "same-origin" }); }
    catch {
      throw new PublicApiRequestError({ code: "NETWORK_UNAVAILABLE", message: translate(readStoredLanguage(), "error.mediaUnavailable"), traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await toApiError(response);
    return response.blob();
  }
}
