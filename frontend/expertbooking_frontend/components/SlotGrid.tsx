import Link from "next/link";
import { StatusPill } from "@/components/StatusPill";
import { appRoutes } from "@/lib/routes";
import { formatTime } from "@/lib/mock/data";
import type { TimeSlotDto } from "@/types/api";

export function SlotGrid({
  expertId,
  slots,
  isAuthenticated,
}: {
  expertId: number | string;
  slots: TimeSlotDto[];
  isAuthenticated: boolean;
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {slots.map((slot) => (
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
