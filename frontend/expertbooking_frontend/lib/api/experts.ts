import { apiRoutes } from "@/lib/routes";
import { apiFetch, type ApiRequestOptions } from "@/lib/api/http";
import type { ExpertDto, SpecialtyDto, TimeSlotDto } from "@/types/api";

export type ExpertListParams = {
  specialty?: string;
  search?: string;
};

export function getExperts(
  authToken: string,
  params: ExpertListParams = {},
  options?: ApiRequestOptions,
) {
  return apiFetch<ExpertDto[]>(apiRoutes.experts, {
    ...options,
    authToken,
    method: "GET",
    query: params,
  });
}

export function getExpert(authToken: string, expertId: number | string, options?: ApiRequestOptions) {
  return apiFetch<ExpertDto>(apiRoutes.expert(expertId), {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getExpertSlots(
  authToken: string,
  expertId: number | string,
  options?: ApiRequestOptions,
) {
  return apiFetch<TimeSlotDto[]>(apiRoutes.expertSlots(expertId), {
    ...options,
    authToken,
    method: "GET",
  });
}

export function getSpecialties(authToken: string, options?: ApiRequestOptions) {
  return apiFetch<SpecialtyDto[]>(apiRoutes.expertSpecialties, {
    ...options,
    authToken,
    method: "GET",
  });
}
