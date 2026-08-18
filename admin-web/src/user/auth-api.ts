import { PublicApiRequestError, toApiError } from "./public-api";
import { CsrfTokenClient } from "../api/csrf";
import { readStoredLanguage, translate } from "../i18n/I18nContext";

export interface AccountSession {
  actorId: string;
  email: string;
  displayName: string;
  roles: string[];
}

export class AuthApi {
  private readonly csrfTokenClient: CsrfTokenClient;

  constructor(private readonly baseUrl = import.meta.env.VITE_API_BASE_URL ?? "") {
    this.csrfTokenClient = new CsrfTokenClient(baseUrl);
  }

  async register(email: string, displayName: string, password: string): Promise<AccountSession> {
    const account = await this.request<AccountSession>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, displayName, password })
    });
    this.csrfTokenClient.clear();
    return account;
  }

  async login(email: string, password: string): Promise<AccountSession> {
    const account = await this.request<AccountSession>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
    this.csrfTokenClient.clear();
    return account;
  }

  async requestPasswordReset(email: string): Promise<void> {
    await this.request<void>("/api/v1/auth/password-reset/request", {
      method: "POST",
      body: JSON.stringify({ email })
    });
  }

  async resetPassword(token: string, newPassword: string): Promise<void> {
    await this.request<void>("/api/v1/auth/password-reset/reset", {
      method: "POST",
      body: JSON.stringify({ token, newPassword })
    });
  }

  me(): Promise<AccountSession> {
    return this.request("/api/v1/auth/me");
  }

  async logout(): Promise<void> {
    await this.request<void>("/api/v1/auth/logout", { method: "POST" });
    this.csrfTokenClient.clear();
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers({ Accept: "application/json" });
    if (init.body !== undefined) headers.set("Content-Type", "application/json");
    await this.csrfTokenClient.protect(headers, init.method);
    let response: Response;
    try {
      response = await fetch(`${this.baseUrl}${path}`, { ...init, headers, credentials: "same-origin" });
    } catch {
      throw new PublicApiRequestError({ code: "NETWORK_UNAVAILABLE", message: translate(readStoredLanguage(), "error.loginUnavailable"), traceId: "unavailable", status: 0 });
    }
    if (!response.ok) throw await toApiError(response);
    if (response.status === 202 || response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }
}
