"use client";

import Link from "next/link";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect, useMemo, useState } from "react";
import { StatusPill } from "@/components/StatusPill";
import { getExpertSlotsTopic, getWebSocketUrl } from "@/lib/api/websocket";
import { appRoutes } from "@/lib/routes";
import { formatTime } from "@/lib/mock/data";
import type { SlotUpdateEvent, TimeSlotDto } from "@/types/api";

export function SlotGrid({
  expertId,
  slots,
  isAuthenticated,
}: {
  expertId: number | string;
  slots: TimeSlotDto[];
  isAuthenticated: boolean;
}) {
  const [slotUpdates, setSlotUpdates] = useState<Record<number, SlotUpdateEvent>>({});
  const liveSlots = useMemo(
    () =>
      slots.map((slot) => {
        const update = slotUpdates[slot.id];
        return update
          ? {
              ...slot,
              status: update.status,
              startTime: update.startTime,
              endTime: update.endTime,
              lockExpiresAt: update.lockExpiresAt,
            }
          : slot;
      }),
    [slotUpdates, slots],
  );

  useEffect(() => {
    const client = new Client({
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS(getWebSocketUrl()),
      onConnect: () => {
        client.subscribe(getExpertSlotsTopic(expertId), (message) => {
          const update = JSON.parse(message.body) as SlotUpdateEvent;
          setSlotUpdates((currentUpdates) => ({ ...currentUpdates, [update.slotId]: update }));
        });
      },
    });

    client.activate();

    return () => {
      void client.deactivate();
    };
  }, [expertId]);

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {liveSlots.map((slot) => (
        <div key={slot.id} className="rounded-lg border border-slate-100 bg-white p-4 shadow-sm">
          <div className="mb-3 flex items-center justify-between gap-3">
            <p className="text-lg font-black text-slate-950">
              {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
            </p>
            <StatusPill status={slot.status} />
          </div>
          <p className="mb-4 text-sm text-slate-500">
            {slot.status === "LOCKED"
              ? "Held temporarily"
              : slot.status === "BOOKED"
                ? "Already booked"
                : isAuthenticated
                  ? "Ready to reserve"
                  : "Sign in to reserve this time"}
          </p>
          {slot.status === "AVAILABLE" ? (
            <Link
              href={
                isAuthenticated
                  ? `${appRoutes.expertCheckout(expertId)}?slotId=${slot.id}`
                  : `${appRoutes.login}?next=${encodeURIComponent(`${appRoutes.expertCheckout(expertId)}?slotId=${slot.id}`)}`
              }
              className="inline-flex w-full items-center justify-center rounded-md bg-emerald-500 px-4 py-2 text-sm font-bold text-white transition hover:bg-emerald-600"
            >
              {isAuthenticated ? "Lock slot" : "Sign in to book"}
            </Link>
          ) : (
            <button
              type="button"
              disabled
              className="w-full rounded-md bg-slate-100 px-4 py-2 text-sm font-bold text-slate-400"
            >
              Unavailable
            </button>
          )}
        </div>
      ))}
    </div>
  );
}
