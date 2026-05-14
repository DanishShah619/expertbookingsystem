"use client";

import { cancelBookingAction } from "@/app/actions/user";
import { useTransition } from "react";

export function CancelBookingButton({ bookingId, expertId }: { bookingId: number; expertId: number }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      onClick={() => {
        if (confirm("Are you sure you want to cancel this booking? This action cannot be undone.")) {
          startTransition(() => {
            cancelBookingAction(bookingId, expertId);
          });
        }
      }}
      disabled={isPending}
      className="rounded-md border border-rose-200 bg-white px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50"
    >
      {isPending ? "Cancelling..." : "Cancel"}
    </button>
  );
}
