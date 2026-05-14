import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { StripeCheckoutPanel } from "@/components/checkout/StripeCheckoutPanel";
import { formatCurrency, formatDateTime } from "@/lib/mock/data";
import { getExpert, getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";

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

  const slot = expertSlots.find((item) => item.id === Number(slotId)) ?? expertSlots[0];

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
