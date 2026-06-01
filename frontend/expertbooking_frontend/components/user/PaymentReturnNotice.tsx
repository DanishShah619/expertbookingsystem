"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { refreshUserBookingsAfterPaymentAction } from "@/app/actions/user";

type PaymentReturnNoticeProps = {
  payment?: string;
  redirectStatus?: string;
  paymentIntentId?: string;
};

export function PaymentReturnNotice({
  payment,
  redirectStatus,
  paymentIntentId,
}: PaymentReturnNoticeProps) {
  const router = useRouter();
  const isSuccessfulReturn = payment === "success" || redirectStatus === "succeeded";
  const isFailedReturn = redirectStatus === "failed" || redirectStatus === "canceled";

  const [isRefreshing, setIsRefreshing] = useState(isSuccessfulReturn);
  const [isConfirmed, setIsConfirmed] = useState(false);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  useEffect(() => {
    if (!isSuccessfulReturn) {
      return;
    }

    // Poll every 2.5 s for up to 90 s (36 attempts).
    // We stop as soon as the server confirms the booking exists in the DB
    // OR we hit the ceiling and warn the user to check manually.
    const MAX_ATTEMPTS = 36; // 36 × 2500 ms = 90 seconds
    let attempts = 0;
    let timer: number | undefined;
    let cancelled = false;

    async function poll() {
      attempts += 1;

      const result = await refreshUserBookingsAfterPaymentAction(paymentIntentId);

      if (cancelled) return;

      if (!result.ok) {
        setRefreshError(result.error ?? "Could not refresh bookings.");
        setIsRefreshing(false);
        window.clearInterval(timer);
        return;
      }

      // Trigger a re-render of the server component so fresh data is displayed
      router.refresh();

      if (result.confirmed) {
        // Booking is in the DB — stop polling
        setIsConfirmed(true);
        setIsRefreshing(false);
        window.clearInterval(timer);
        return;
      }

      if (attempts >= MAX_ATTEMPTS) {
        // 90 s elapsed and still not confirmed — Stripe webhook may be very delayed
        setIsRefreshing(false);
        window.clearInterval(timer);
      }
    }

    // Run immediately, then repeat
    poll();
    timer = window.setInterval(poll, 2500);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [isSuccessfulReturn, paymentIntentId, router]);

  if (isFailedReturn) {
    return (
      <div className="mb-5 rounded-lg border border-rose-100 bg-rose-50 p-4 text-sm font-semibold text-rose-700">
        Payment was not completed. Please choose the slot again and retry payment.
      </div>
    );
  }

  if (!isSuccessfulReturn) {
    return null;
  }

  return (
    <div className="mb-5 rounded-lg border border-emerald-100 bg-emerald-50 p-4">
      <p className="text-sm font-black text-emerald-800">Payment successful</p>
      <p className="mt-1 text-sm leading-6 text-emerald-700">
        Your payment was received. We are confirming your booking with Stripe now.
      </p>
      {isRefreshing ? (
        <p className="mt-2 text-xs font-bold uppercase tracking-wide text-emerald-600">
          Waiting for booking confirmation...
        </p>
      ) : isConfirmed ? (
        <p className="mt-2 text-sm font-semibold text-emerald-700">
          ✓ Booking confirmed and visible below.
        </p>
      ) : refreshError ? (
        <p className="mt-2 text-sm font-semibold text-rose-700">{refreshError}</p>
      ) : (
        <p className="mt-2 text-sm font-semibold text-amber-700">
          Booking confirmation is taking longer than expected. If your booking is missing,
          please wait a moment and refresh the page, or check the Stripe webhook on your
          backend hosting dashboard.
        </p>
      )}
    </div>
  );
}

