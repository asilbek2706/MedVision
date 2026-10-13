"use client";

export type CurrentUser = {
  id: string;
  name: string;
  email: string;
  createdAt: string;
};

type CsrfResponse = {
  headerName: string;
  parameterName: string;
  token: string;
};

export class ApiRequestError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(path, {
    ...options,
    credentials: "same-origin",
    cache: "no-store",
    redirect: "error",
  });

  if (!response.ok) {
    let code = "REQUEST_FAILED";
    let message = `So'rov bajarilmadi (${response.status}).`;

    try {
      const body: unknown = await response.json();
      if (typeof body === "object" && body !== null) {
        if ("code" in body && typeof body.code === "string") {
          code = body.code;
        }
        if ("message" in body && typeof body.message === "string") {
          message = body.message;
        }
      }
    } catch {
      // Preserve the HTTP status if the proxy returns non-JSON content.
    }

    throw new ApiRequestError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const csrf = await request<CsrfResponse>("/api/auth/csrf");
  const headers = new Headers({ Accept: "application/json" });

  if (csrf.headerName !== "X-CSRF-TOKEN" || !csrf.token) {
    throw new Error("CSRF javobi noto'g'ri.");
  }

  headers.set("X-CSRF-TOKEN", csrf.token);

  if (body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  return request<T>(path, {
    method: "POST",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export const authApi = {
  me: () => request<CurrentUser>("/api/auth/me"),
  register: (name: string, email: string, password: string) =>
    post<CurrentUser>("/api/auth/register", { name, email, password }),
  login: (email: string, password: string) =>
    post<CurrentUser>("/api/auth/login", { email, password }),
  logout: () => post<void>("/api/auth/logout"),
};