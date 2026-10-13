"use client";

import type {
  MedicalImage,
  AnalysisOptions,
  AnalysisParameters,
  AnalysisResult,
} from "./segmentation-types";


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

async function csrfHeaders(): Promise<Headers> {
  const csrf = await request<CsrfResponse>("/api/auth/csrf");

  if (csrf.headerName !== "X-CSRF-TOKEN" || !csrf.token) {
    throw new Error("CSRF javobi noto'g'ri.");
  }

  return new Headers({
    Accept: "application/json",
    "X-CSRF-TOKEN": csrf.token,
  });
}

async function postMultipart<T>(
  path: string,
  formData: FormData,
): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: await csrfHeaders(),
    body: formData,
  });
}

async function getBlob(path: string): Promise<Blob> {
  const response = await fetch(path, {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    redirect: "error",
  });

  if (!response.ok) {
    let code = "REQUEST_FAILED";
    let message = `Faylni olishda xatolik (${response.status}).`;

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
      // Preserve the HTTP status when the response is not JSON.
    }

    throw new ApiRequestError(response.status, code, message);
  }

  return response.blob();
}

export const segmentationApi = {
  options: () =>
    request<AnalysisOptions>("/api/analysis-options"),

  upload: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return postMultipart<MedicalImage>("/api/images", formData);
  },

  validate: (parameters: AnalysisParameters) =>
    post<AnalysisParameters>(
      "/api/analysis-options/validate",
      parameters,
    ),

  execute: (imageId: string, parameters: AnalysisParameters) =>
    post<AnalysisResult>(
      `/api/images/${encodeURIComponent(imageId)}/analyses`,
      parameters,
    ),

  original: (imageId: string) =>
    getBlob(
      `/api/images/${encodeURIComponent(imageId)}/content`,
    ),

  mask: (analysisId: string) =>
    getBlob(
      `/api/analyses/${encodeURIComponent(analysisId)}/mask`,
    ),

  overlay: (analysisId: string) =>
    getBlob(
      `/api/analyses/${encodeURIComponent(analysisId)}/overlay`,
    ),

  downloadMask: (analysisId: string) =>
    getBlob(
      `/api/analyses/${encodeURIComponent(analysisId)}/mask/download`,
    ),

  downloadOverlay: (analysisId: string) =>
    getBlob(
      `/api/analyses/${encodeURIComponent(analysisId)}/overlay/download`,
    ),
};
