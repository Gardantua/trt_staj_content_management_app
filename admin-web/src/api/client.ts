import type { LocalActor } from "../auth/actor";
import { CsrfTokenClient } from "./csrf";
import { readStoredLanguage, translate } from "../i18n/I18nContext";

export interface ApiError {
  code: string;
  message: string;
  traceId: string;
  status: number;
}

export class ApiRequestError extends Error implements ApiError {
  code: string;
  traceId: string;
  status: number;

  constructor(error: ApiError) {
    super(error.message);
    this.name = "ApiRequestError";
    this.code = error.code;
    this.traceId = error.traceId;
    this.status = error.status;
  }
}

interface ApiErrorBody {
  code?: unknown;
  message?: unknown;
  traceId?: unknown;
}

function asApiErrorBody(value: unknown): ApiErrorBody {
  return typeof value === "object" && value !== null ? value as ApiErrorBody : {};
}

export async function readApiError(response: Response): Promise<ApiRequestError> {
  let body: ApiErrorBody = {};
  try {
    body = asApiErrorBody(await response.json());
  } catch {
    // Non-JSON proxy and network errors still receive a safe, actionable presentation.
  }

  return new ApiRequestError({
    code: typeof body.code === "string" ? body.code : "UNEXPECTED_API_ERROR",
    message: typeof body.message === "string" ? body.message : translate(readStoredLanguage(), "error.requestFailed"),
    traceId: typeof body.traceId === "string" ? body.traceId : "unavailable",
    status: response.status
  });
}

export class ApiClient {
  private readonly csrfTokenClient: CsrfTokenClient;

  constructor(
    private readonly actor: LocalActor | null = null,
    private readonly baseUrl = import.meta.env.VITE_API_BASE_URL ?? ""
  ) {
    this.csrfTokenClient = new CsrfTokenClient(baseUrl);
  }

  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");
    headers.set("Accept-Language", readStoredLanguage());
    if (init.body !== undefined) {
      headers.set("Content-Type", "application/json");
    }
    if (this.actor?.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    if (!this.actor?.id) await this.csrfTokenClient.protect(headers, init.method);

    let response: Response;
    try {
      response = await fetch(`${this.baseUrl}${path}`, {
        ...init, headers, credentials: "same-origin"
      });
    } catch {
      throw new ApiRequestError({
        code: "NETWORK_UNAVAILABLE",
        message: translate(readStoredLanguage(), "error.adminUnavailable"),
        traceId: "unavailable",
        status: 0
      });
    }

    if (!response.ok) {
      throw await readApiError(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return response.json() as Promise<T>;
  }

  async requestForm<T>(path: string, formData: FormData, init: Omit<RequestInit, "body" | "headers"> = {}): Promise<T> {
    const headers = new Headers({ Accept: "application/json", "Accept-Language": readStoredLanguage() });
    if (this.actor?.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    if (!this.actor?.id) await this.csrfTokenClient.protect(headers, init.method);

    let response: Response;
    try {
      response = await fetch(`${this.baseUrl}${path}`, {
        ...init, body: formData, headers, credentials: "same-origin"
      });
    } catch {
      throw new ApiRequestError({
        code: "NETWORK_UNAVAILABLE",
        message: translate(readStoredLanguage(), "error.adminUnavailable"),
        traceId: "unavailable",
        status: 0
      });
    }

    if (!response.ok) {
      throw await readApiError(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return response.json() as Promise<T>;
  }

  async requestBlob(path: string): Promise<Blob> {
    const headers = new Headers({ "Accept-Language": readStoredLanguage() });
    if (this.actor?.id) {
      headers.set("X-Test-Actor-Id", this.actor.id);
      headers.set("X-Test-Actor-Roles", this.actor.roles.join(","));
    }
    let response: Response;
    try {
      response = await fetch(`${this.baseUrl}${path}`, {
        headers, credentials: "same-origin"
      });
    } catch {
      throw new ApiRequestError({ code: "NETWORK_UNAVAILABLE", message: translate(readStoredLanguage(), "error.imageUnavailable"), traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await readApiError(response);
    return response.blob();
  }
}
