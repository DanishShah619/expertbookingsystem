"use client";

import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import type { StripeElementsOptions } from "@stripe/stripe-js";
import { lockSlotForCheckoutAction, releaseSlotForCheckoutAction } from "@/app/actions/checkout";
import { appRoutes } from "@/lib/routes";
import type { CurrencyCode, SlotLockResponse, SlotStatus } from "@/types/api";

const stripePublishableKey = process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY;
const stripePromise = stripePublishableKey ? loadStripe(stripePublishableKey) : null;

export function StripeCheckoutPanel({
  expertId,
  slotId,
  slotStatus,
  amount,
  currency,
}: {
  expertId: number;
  slotId: number | null;
  slotStatus: SlotStatus | string | null;
  amount: number;
  currency: CurrencyCode;
}) {
  const router = useRouter();
  const [lock, setLock] = useState<SlotLockResponse | null>(null);
  const [isLocking, setIsLocking] = useState(false);
  const [isReleasing, setIsReleasing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLockExpired, setIsLockExpired] = useState(false);

  const canStartPayment = Boolean(slotId && slotStatus === "AVAILABLE" && stripePromise);
  const clientSecret = lock?.clientSecret;
  const handleLockExpired = useCallback(() => setIsLockExpired(true), []);

  const options = useMemo<StripeElementsOptions | null>(() => {
    if (!clientSecret) {
      return null;
    }

    return {
      clientSecret,
      appearance: {
        theme: "stripe",
        variables: {
          colorPrimary: "#059669",
          borderRadius: "8px",
          fontFamily: "Inter, system-ui, sans-serif",
        },
      },
    };
  }, [clientSecret]);

  async function handleStartPayment() {
    if (!slotId) {
      setError("Please select a slot before paying.");
      return;
    }

    setIsLocking(true);
    setError(null);

    const result = await lockSlotForCheckoutAction(slotId, expertId);

    if (result.error) {
      setError(result.error);
      setIsLocking(false);
      return;
    }

    setLock(result.data);
    setIsLockExpired(false);
    setIsLocking(false);
  }

  async function handleCancel() {
    if (!slotId || !lock) {
      router.push(appRoutes.expertDetail(expertId));
      return;
    }

    setIsReleasing(true);
    await releaseSlotForCheckoutAction(slotId, expertId);
    router.push(appRoutes.expertDetail(expertId));
  }

  if (!stripePromise) {
    return (
      <PaymentShell>
        <Alert message="Stripe is not configured. Set NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY in Vercel and redeploy." />
      </PaymentShell>
    );
  }

  if (!slotId) {
    return (
      <PaymentShell>
        <Alert message="No slot was selected. Go back to the expert profile and choose an available slot." />
      </PaymentShell>
    );
  }

  if (slotStatus !== "AVAILABLE" && !lock) {
    return (
      <PaymentShell>
        <Alert message="This slot is no longer available. Please choose another time." />
      </PaymentShell>
    );
  }

  return (
    <PaymentShell>
      {lock && (
        <div className="mb-5 rounded-lg bg-emerald-50 p-4">
          <p className="text-sm font-bold text-emerald-700">Lock expires in</p>
          <LockCountdown expiresAt={lock.expiresAt} onExpire={handleLockExpired} />
        </div>
      )}

      {!lock || !options ? (
        <div className="space-y-4">
          <div className="rounded-lg border border-dashed border-slate-200 p-6 text-center">
            <p className="font-bold text-slate-900">Secure Stripe checkout</p>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Your slot will be held while you complete payment.
            </p>
          </div>
          {error ? <Alert message={error} /> : null}
          <button
            type="button"
            disabled={!canStartPayment || isLocking}
            onClick={handleStartPayment}
            className="inline-flex w-full items-center justify-center rounded-md bg-emerald-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {isLocking ? "Preparing payment..." : `Pay ${formatDisplayAmount(amount, currency)}`}
          </button>
        </div>
      ) : (
        <Elements stripe={stripePromise} options={options}>
          <CheckoutForm
            onCancel={handleCancel}
            isReleasing={isReleasing}
            isLockExpired={isLockExpired}
            lockExpiresAt={lock.expiresAt}
          />
        </Elements>
      )}
    </PaymentShell>
  );
}

function CheckoutForm({
  onCancel,
  isReleasing,
  isLockExpired,
  lockExpiresAt,
}: {
  onCancel: () => void;
  isReleasing: boolean;
  isLockExpired: boolean;
  lockExpiresAt: string;
}) {
  const stripe = useStripe();
  const elements = useElements();
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    if (Date.now() >= new Date(lockExpiresAt).getTime()) {
      setError("This slot lock has expired. Please choose the slot again before paying.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: `${window.location.origin}${appRoutes.myBookings}?payment=success`,
      },
      redirect: "if_required",
    });

    if (result.error) {
      setError(result.error.message ?? "Payment failed. Please check your details and try again.");
      setIsSubmitting(false);
      return;
    }

    const paymentIntentId = result.paymentIntent?.id;
    const query = new URLSearchParams({ payment: "success" });

    if (paymentIntentId) {
      query.set("payment_intent", paymentIntentId);
    }

    router.push(`${appRoutes.myBookings}?${query.toString()}`);
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <PaymentElement />
      {isLockExpired ? <Alert message="This slot lock has expired. Please choose the slot again before paying." /> : null}
      {error ? <Alert message={error} /> : null}
      <button
        type="submit"
        disabled={!stripe || !elements || isSubmitting || isLockExpired}
        className="inline-flex w-full items-center justify-center rounded-md bg-emerald-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
      >
        {isSubmitting ? "Confirming payment..." : "Confirm payment"}
      </button>
      <button
        type="button"
        disabled={isSubmitting || isReleasing}
        onClick={onCancel}
        className="inline-flex w-full items-center justify-center rounded-md border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isReleasing ? "Releasing slot..." : "Cancel and release slot"}
      </button>
    </form>
  );
}

function LockCountdown({ expiresAt, onExpire }: { expiresAt: string; onExpire: () => void }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const remainingSeconds = Math.max(0, Math.floor((new Date(expiresAt).getTime() - now) / 1000));
  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = String(remainingSeconds % 60).padStart(2, "0");

  useEffect(() => {
    if (remainingSeconds === 0) {
      onExpire();
    }
  }, [onExpire, remainingSeconds]);

  return <p className="mt-1 text-3xl font-black text-slate-950">{minutes}:{seconds}</p>;
}

function PaymentShell({ children }: { children: ReactNode }) {
  return <div className="rounded-lg border border-emerald-100 bg-white p-6 shadow-sm">{children}</div>;
}

function Alert({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-rose-100 bg-rose-50 p-4 text-sm font-semibold text-rose-700">
      {message}
    </div>
  );
}

function formatDisplayAmount(amount: number, currency: string) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}
