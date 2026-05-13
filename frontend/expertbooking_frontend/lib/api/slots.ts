import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { SlotLockResponse } from "@/types/api";

export function lockSlot(authToken: string, slotId: number | string, options?: ApiRequestOptions) {
  return apiFetch<SlotLockResponse>(apiRoutes.slotLock(slotId), {
    ...options,
    authToken,
    method: "POST",
  });
}

export function releaseSlotLock(
  authToken: string,
  slotId: number | string,
  options?: ApiRequestOptions,
) {
  return apiFetch<void>(apiRoutes.slotLock(slotId), {
    ...options,
    authToken,
    method: "DELETE",
  });
}
