import type { LocalActor } from "../auth/actor";
import type { AnswerSubmissionResult, CurrentActor, Leaderboard, PublicApiError, PublicContent, PublicContentPage, PublishedQuiz, QuizAttempt, XpSummary } from "./types";

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

async function toApiError(response: Response): Promise<PublicApiRequestError> {
  let body: ErrorBody = {};
  try { body = await response.json() as ErrorBody; } catch { /* A proxy error can be plain text. */ }
  return new PublicApiRequestError({
    code: typeof body.code === "string" ? body.code : "UNEXPECTED_API_ERROR",
    message: typeof body.message === "string" ? body.message : "İstek tamamlanamadı.",
    traceId: typeof body.traceId === "string" ? body.traceId : "unavailable",
    status: response.status
  });
}

export class PublicApi {
  constructor(
    private readonly actor: LocalActor,
    private readonly baseUrl = import.meta.env.VITE_API_BASE_URL ?? ""
  ) {}

  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers({ Accept: "application/json" });
    new Headers(init.headers).forEach((value, name) => headers.set(name, value));
    if (init.body !== undefined) headers.set("Content-Type", "application/json");
    if (this.actor.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    let response: Response;
    try { response = await fetch(`${this.baseUrl}${path}`, { ...init, headers }); }
    catch {
      throw new PublicApiRequestError({ code: "NETWORK_UNAVAILABLE", message: "İçerik servisine ulaşılamadı.", traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await toApiError(response);
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }

  get<T>(path: string): Promise<T> { return this.request(path); }

  listContents(page = 0, size = 12): Promise<PublicContentPage> {
    return this.get(`/api/v1/contents?page=${page}&size=${size}`);
  }

  getContent(contentId: string): Promise<PublicContent> {
    return this.get(`/api/v1/contents/${contentId}`);
  }

  listQuizzes(contentId: string): Promise<PublishedQuiz[]> {
    return this.get(`/api/v1/contents/${contentId}/quizzes`);
  }

  getQuiz(quizId: string): Promise<PublishedQuiz> { return this.get(`/api/v1/quizzes/${quizId}`); }
  startAttempt(quizId: string, timingPolicyVersion: QuizAttempt["timingPolicyVersion"]): Promise<QuizAttempt> {
    return this.request(`/api/v1/quizzes/${quizId}/attempts`, { method: "POST", body: JSON.stringify({ timingPolicyVersion }) });
  }
  submitAnswer(attemptId: string, questionId: string, selectedOptionId: string, idempotencyKey: string): Promise<AnswerSubmissionResult> {
    return this.request(`/api/v1/attempts/${attemptId}/answers`, { method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body: JSON.stringify({ questionId, selectedOptionId }) });
  }
  getXp(): Promise<XpSummary> { return this.get("/api/v1/me/xp"); }
  getIdentity(): Promise<CurrentActor> { return this.get("/api/v1/identity/me"); }
  getGlobalLeaderboard(): Promise<Leaderboard> { return this.get("/api/v1/leaderboards/global?limit=10"); }

  async getMedia(path: string): Promise<Blob> {
    const headers = new Headers({ Accept: "image/*" });
    if (this.actor.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    let response: Response;
    try { response = await fetch(`${this.baseUrl}${path}`, { headers }); }
    catch {
      throw new PublicApiRequestError({ code: "NETWORK_UNAVAILABLE", message: "Görsel servisine ulaşılamadı.", traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await toApiError(response);
    return response.blob();
  }
}
