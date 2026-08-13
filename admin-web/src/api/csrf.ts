export interface CsrfTokenResponse {
  headerName: string;
  token: string;
}

interface CsrfEndpointResponse { headerName: string; }

const safeMethods = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

export class CsrfTokenClient {
  private tokenRequest: Promise<CsrfTokenResponse> | null = null;

  constructor(private readonly baseUrl: string) {}

  async protect(headers: Headers, method = "GET"): Promise<void> {
    if (safeMethods.has(method.toUpperCase())) return;
    const csrfToken = await this.getToken();
    headers.set(csrfToken.headerName, csrfToken.token);
  }

  clear(): void {
    this.tokenRequest = null;
  }

  private getToken(): Promise<CsrfTokenResponse> {
    if (!this.tokenRequest) {
      this.tokenRequest = fetch(`${this.baseUrl}/api/v1/auth/csrf`, {
        headers: { Accept: "application/json" },
        credentials: "same-origin"
      }).then(async (response) => {
        if (!response.ok) throw new Error("CSRF token could not be obtained.");
        const responseToken = await response.json() as CsrfEndpointResponse;
        const cookieToken = readCookie("XSRF-TOKEN");
        if (!cookieToken) throw new Error("CSRF cookie could not be obtained.");
        return { headerName: responseToken.headerName, token: cookieToken };
      }).catch((error: unknown) => {
        this.tokenRequest = null;
        throw error;
      });
    }
    return this.tokenRequest;
  }
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  const cookie = document.cookie.split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}
