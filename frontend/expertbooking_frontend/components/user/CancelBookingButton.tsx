"use client";

import { cancelBookingAction } from "@/app/actions/user";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

export function CancelBookingButton({ bookingId, expertId }: { bookingId: number; expertId: number }) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  return (
    <div className="flex flex-col items-start gap-2">
      <button
        onClick={() => {
          setError(null);
          if (confirm("Are you sure you want to cancel this booking? This action cannot be undone.")) {
            startTransition(() => {
              void cancelBookingAction(bookingId, expertId).then((result) => {
                if (!result.ok) {
                  setError(result.error);
                  return;
                }
                router.refresh();
              });
            });
          }
        }}
        disabled={isPending}
        className="rounded-md border border-rose-200 bg-white px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50"
      >
        {isPending ? "Cancelling..." : "Cancel"}
      </button>
      {error ? <p className="max-w-xs text-xs font-semibold text-rose-700">{error}</p> : null}
    </div>
  );
}
