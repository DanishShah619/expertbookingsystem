import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type {
  BookingDto,
  ExpertCreateRequest,
  ExpertDto,
  SlotCreateRequest,
  SpecialtyDto,
  TimeSlotDto,
} from "@/types/api";

export function getAdminExperts(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<ExpertDto[]>(apiRoutes.adminExperts, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function createExpert(
  authToken: string,
  request: ExpertCreateRequest,
  options?: ApiRequestOptions,
) {
  return apiFetch<ExpertDto>(apiRoutes.adminExperts, {
    ...options,
    authToken,
    method: "POST",
    body: request,
  });
}

export function updateExpert(
  authToken: string,
  expertId: number | string,
  request: ExpertCreateRequest,
  options?: ApiRequestOptions,
) {
  return apiFetch<ExpertDto>(apiRoutes.adminExpert(expertId), {
    ...options,
    authToken,
    method: "PUT",
    body: request,
  });
}

export function deleteExpert(authToken: string, expertId: number | string, options?: ApiRequestOptions) {
  return apiFetch<void>(apiRoutes.adminExpert(expertId), {
    ...options,
    authToken,
    method: "DELETE",
  });
}

export function getAdminSpecialties(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<SpecialtyDto[]>(apiRoutes.adminSpecialties, {
    ...options,
    authToken,
    method: "GET",
  });
}

export function createSpecialty(authToken: string, name: string, options?: ApiRequestOptions) {
  return apiFetch<SpecialtyDto>(apiRoutes.adminSpecialties, {
    ...options,
    authToken,
    method: "POST",
    query: { name },
  });
}

export function deleteSpecialty(
  authToken: string,
  specialtyId: number | string,
  options?: ApiRequestOptions,
) {
  return apiFetch<void>(apiRoutes.adminSpecialty(specialtyId), {
    ...options,
    authToken,
    method: "DELETE",
  });
}

export function createSlot(
  authToken: string,
  request: SlotCreateRequest,
  options?: ApiRequestOptions,
) {
  return apiFetch<TimeSlotDto>(apiRoutes.adminSlots, {
    ...options,
    authToken,
    method: "POST",
    body: request,
  });
}

export function deleteSlot(authToken: string, slotId: number | string, options?: ApiRequestOptions) {
  return apiFetch<void>(apiRoutes.adminSlot(slotId), {
    ...options,
    authToken,
    method: "DELETE",
  });
}

export function getAdminBookings(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<BookingDto[]>(apiRoutes.adminBookings, {
    ...options,
    authToken,
    method: "GET",
  });
}
