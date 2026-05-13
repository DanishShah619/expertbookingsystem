import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { UserDto } from "@/types/api";

export function getCurrentAuthUser(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<UserDto>(apiRoutes.authMe, {
    ...options,
    authToken,
    method: "GET",
  });
}
