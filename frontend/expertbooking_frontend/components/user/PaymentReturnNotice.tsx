"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { refreshUserBookingsAfterPaymentAction } from "@/app/actions/user";

type PaymentReturnNoticeProps = {
  payment?: string;
  redirectStatus?: string;
};

export function PaymentReturnNotice({ payment, redirectStatus }: PaymentReturnNoticeProps) {
  const router = useRouter();
  const isSuccessfulReturn = payment === "success" || redirectStatus === "succeeded";
  const isFailedReturn = redirectStatus === "failed" || redirectStatus === "canceled";
  const [isRefreshing, setIsRefreshing] = useState(isSuccessfulReturn);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  useEffect(() => {
    if (!isSuccessfulReturn) {
      return;
    }

    let cancelled = false;
    let attempts = 0;

    async function refreshBookings() {
      attempts += 1;
      const result = await refreshUserBookingsAfterPaymentAction();

      if (cancelled) {
        return;
      }

      if (!result.ok) {
        setRefreshError(result.error ?? "Could not refresh bookings.");
        setIsRefreshing(false);
        return;
      }

      router.refresh();

      if (attempts >= 8) {
        setIsRefreshing(false);
      }
    }

    refreshBookings();
    const timer = window.setInterval(refreshBookings, 2500);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [isSuccessfulReturn, router]);

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
        Your payment was received. We are refreshing this page while Stripe confirms the booking.
      </p>
      {isRefreshing ? (
        <p className="mt-2 text-xs font-bold uppercase tracking-wide text-emerald-600">
          Updating bookings...
        </p>
      ) : refreshError ? (
        <p className="mt-2 text-sm font-semibold text-rose-700">{refreshError}</p>
      ) : (
        <p className="mt-2 text-sm font-semibold text-emerald-700">
          Bookings refreshed. If the new booking is still missing, check the Stripe webhook on Render.
        </p>
      )}
    </div>
  );
}
