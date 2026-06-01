import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { StripeCheckoutPanel } from "@/components/checkout/StripeCheckoutPanel";
import { formatCurrency, formatDateTime } from "@/lib/mock/data";
import { getExpert, getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";
import Link from "next/link";

export default async function CheckoutPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ slotId?: string }>;
}) {
  const { id } = await params;
  const { slotId } = await searchParams;
  const token = await getServerAuthToken();

  const expert = await getExpert(token, id, {
    next: { revalidate: 3600, tags: [cacheTags.expert(id)] },
  }).catch(() => null);

  const expertSlots = await getExpertSlots(token, id, {
    next: { revalidate: 0, tags: [cacheTags.expertSlots(id)] },
  }).catch(() => []);

  if (!expert) {
    return (
      <AppShell>
        <PageHeader eyebrow="Error" title="Expert not found" />
      </AppShell>
    );
  }

  const slot = slotId
    ? expertSlots.find((item) => item.id === Number(slotId)) ?? null
    : null;

  if (!slot) {
    return (
      <AppShell>
        <PageHeader
          eyebrow="Error"
          title="Slot not found or expired"
          description="The selected slot is invalid, has expired, or is already booked."
        />
        <div className="max-w-md mx-auto mt-8 rounded-xl border border-slate-100 bg-white p-8 text-center shadow-sm">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rose-50 text-rose-500 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
            </svg>
          </div>
          <h3 className="text-lg font-bold text-slate-900 mb-2">Invalid or missing slot</h3>
          <p className="text-sm text-slate-500 mb-6">
            We couldn't retrieve the selected session slot. It might have been booked by someone else or your session timed out.
          </p>
          <Link
            href={`/experts/${expert.id}`}
            className="inline-flex items-center justify-center rounded-lg bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-950 transition-all duration-200"
          >
            Go back to expert slots
          </Link>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Checkout"
        title="Complete payment before the slot lock expires"
        description="Review your booking, hold the slot, and confirm payment securely with Stripe."
      />

      <section className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="rounded-lg border border-slate-100 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-xl font-black text-slate-950">Booking summary</h2>
          <dl className="space-y-4 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">Expert</dt>
              <dd className="font-bold text-slate-900">{expert.name}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">Slot</dt>
              <dd className="font-bold text-slate-900">{slot ? formatDateTime(slot.startTime) : "Select a slot"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">Status</dt>
              <dd>{slot ? <StatusPill status={slot.status} /> : null}</dd>
            </div>
            <div className="flex items-center justify-between gap-4 border-t border-slate-100 pt-4">
              <dt className="text-slate-500">Amount</dt>
              <dd className="text-2xl font-black text-slate-950">
                {formatCurrency(expert.sessionPrice, expert.currency)}
              </dd>
            </div>
          </dl>
        </div>

        <StripeCheckoutPanel
          expertId={expert.id}
          slotId={slot?.id ?? null}
          slotStatus={slot?.status ?? null}
          amount={expert.sessionPrice}
          currency={expert.currency}
        />
      </section>
    </AppShell>
  );
}
