import { API_BASE_URL } from "@/lib/api/config";
import type { ApiErrorBody } from "@/types/api";

type QueryValue = string | number | boolean | null | undefined;

export type ApiQuery = Record<string, QueryValue>;

export type ApiRequestOptions = Omit<RequestInit, "body"> & {
  authToken?: string | null;
  body?: unknown;
  query?: ApiQuery;
  next?: {
    revalidate?: number | false;
    tags?: string[];
  };
};

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | string | null;
  readonly url: string;

  constructor(status: number, message: string, body: ApiErrorBody | string | null, url: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
    this.url = url;
  }
}

export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { authToken, body, headers, query, ...fetchOptions } = options;
  const url = buildApiUrl(path, query);
  const requestHeaders = new Headers(headers);

  if (authToken) {
    requestHeaders.set("Authorization", `Bearer ${authToken}`);
  }

  const requestInit: RequestInit & { next?: ApiRequestOptions["next"] } = {
    ...fetchOptions,
    headers: requestHeaders,
  };

  if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    requestInit.body = JSON.stringify(body);
  }

  const response = await fetch(url, requestInit);

  if (response.status === 204) {
    return undefined as T;
  }

  const responseBody = await parseResponseBody(response);

  if (!response.ok) {
    throw new ApiError(response.status, getApiErrorMessage(responseBody, response.status), responseBody, url);
  }

  return responseBody as T;
}

export function buildApiUrl(path: string, query?: ApiQuery): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const url = new URL(`${API_BASE_URL}${normalizedPath}`);

  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  });

  return url.toString();
}

async function parseResponseBody(response: Response): Promise<ApiErrorBody | string | null> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as ApiErrorBody;
  } catch {
    return text;
  }
}

function getApiErrorMessage(body: ApiErrorBody | string | null, status: number): string {
  if (typeof body === "string") {
    return body;
  }

  if (body && typeof body === "object") {
    const message = typeof body.message === "string" ? body.message.trim() : "";
    if (message) {
      return message;
    }

    const error = typeof body.error === "string" ? body.error.trim() : "";
    if (error) {
      return error;
    }
  }

  return `Request failed with status ${status}`;
}
