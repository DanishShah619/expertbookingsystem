import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";

export function cancelBooking(
  authToken: string,
  bookingId: number | string,
  options?: ApiRequestOptions,
) {
  return apiFetch<void>(apiRoutes.booking(bookingId), {
    ...options,
    authToken,
    method: "DELETE",
  });
}

