import type { BookingStatus, SlotStatus } from "@/types/api";

export function StatusPill({ status }: { status: SlotStatus | BookingStatus | string }) {
  const className =
    status === "AVAILABLE"
      ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
      : status === "LOCKED"
        ? "bg-amber-50 text-amber-700 ring-amber-200"
        : status === "BOOKED"
          ? "bg-slate-100 text-slate-600 ring-slate-200"
          : status === "CONFIRMED"
            ? "bg-cyan-50 text-cyan-700 ring-cyan-200"
            : status === "CANCELLED"
              ? "bg-rose-50 text-rose-700 ring-rose-200"
              : "bg-violet-50 text-violet-700 ring-violet-200";

  return (
    <span className={`inline-flex rounded-md px-2.5 py-1 text-xs font-bold ring-1 ${className}`}>
      {status}
    </span>
  );
}
