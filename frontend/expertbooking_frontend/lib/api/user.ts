import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { BookingDto, UserProfileDto } from "@/types/api";

export function getUserProfile(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<UserProfileDto>(apiRoutes.userMe, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getUserBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<BookingDto[]>(apiRoutes.userBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getUserUpcomingBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<BookingDto[]>(apiRoutes.userUpcomingBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getUserPastBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<BookingDto[]>(apiRoutes.userPastBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}
