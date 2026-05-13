import { WS_BASE_URL } from "@/lib/api/config";
import { wsRoutes } from "@/lib/routes";

export function getWebSocketUrl(): string {
  return WS_BASE_URL;
}

export function getExpertSlotsTopic(expertId: number | string): string {
  return wsRoutes.expertSlotsTopic(expertId);
}
