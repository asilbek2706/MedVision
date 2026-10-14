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

  evaluate: (analysisId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    return postMultipart<AnalysisResult>(
      `/api/analyses/${encodeURIComponent(analysisId)}/evaluate`,
      formData,
    );
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

export type DashboardStats = {
  totalImages: number;
  completedAnalyses: number;
  failedAnalyses: number;
};

export type AnalysisStatus = "PROCESSING" | "COMPLETED" | "FAILED";

export type AnalysisHistoryItem = {
  id: string;
  name: string;
  imageId: string;
  algorithm: string;
  parameters: Record<string, unknown>;
  status: AnalysisStatus;
  foregroundPixels: number | null;
  foregroundRatio: number | null;
  processingTimeMs: number | null;
  diceScore: number | null;
  iouScore: number | null;
  hasGroundTruth: boolean;
  errorCode: string | null;
  createdAt: string;
  completedAt: string | null;
};

export type MedicalImageItem = {
  id: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  width: number;
  height: number;
  createdAt: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type AnalysisHistoryFilters = {
  page?: number;
  size?: number;
  status?: AnalysisStatus;
  algorithm?: string;
  from?: string;
  to?: string;
};

export const dashboardApi = {
  renameAnalysis: async (id: string, name: string) => {
    const headers = await csrfHeaders();
    headers.set("Content-Type", "application/json");

    return request<AnalysisHistoryItem>(
      `/api/analyses/${encodeURIComponent(id)}`,
      {
        method: "PATCH",
        headers,
        body: JSON.stringify({ name }),
      }
    );
  },
  stats: () =>
    request<DashboardStats>("/api/dashboard/stats"),

  recentAnalyses: (size = 5) =>
    request<PageResponse<AnalysisHistoryItem>>(
      `/api/analyses?page=0&size=${size}`,
    ),

  analyses: (filters: AnalysisHistoryFilters = {}) => {
    const params = new URLSearchParams();

    params.set("page", String(filters.page ?? 0));
    params.set("size", String(filters.size ?? 20));

    if (filters.status) params.set("status", filters.status);
    if (filters.algorithm) params.set("algorithm", filters.algorithm);
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);

    return request<PageResponse<AnalysisHistoryItem>>(
      `/api/analyses?${params.toString()}`,
    );
  },

  analysis: (id: string) =>
    request<AnalysisHistoryItem>(
      `/api/analyses/${encodeURIComponent(id)}`,
    ),

  images: (page = 0, size = 20) =>
    request<PageResponse<MedicalImageItem>>(
      `/api/images?page=${page}&size=${size}`,
    ),

  image: (id: string) =>
    request<MedicalImageItem>(
      `/api/images/${encodeURIComponent(id)}`,
    ),

  imageAnalyses: (id: string, page = 0, size = 20) =>
    request<PageResponse<AnalysisHistoryItem>>(
      `/api/images/${encodeURIComponent(id)}/analyses?page=${page}&size=${size}`,
    ),
};
