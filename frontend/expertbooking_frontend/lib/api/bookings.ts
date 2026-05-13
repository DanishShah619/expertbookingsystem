import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { BookingDto } from "@/types/api";

export function getMyBookingsLegacy(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<BookingDto[]>(apiRoutes.bookingMeLegacy, {
    ...options,
    authToken,
    method: "GET",
  });
}

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
