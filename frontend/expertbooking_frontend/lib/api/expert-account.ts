import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { ExpertBookingDto, ExpertProfileDto } from "@/types/api";

export function getExpertProfile(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<ExpertProfileDto>(apiRoutes.expertMe, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getExpertBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<ExpertBookingDto[]>(apiRoutes.expertBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getExpertUpcomingBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<ExpertBookingDto[]>(apiRoutes.expertUpcomingBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getExpertTodayBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<ExpertBookingDto[]>(apiRoutes.expertTodayBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}
