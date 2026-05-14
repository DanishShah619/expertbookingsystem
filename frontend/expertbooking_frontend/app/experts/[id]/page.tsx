import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { SlotGrid } from "@/components/SlotGrid";
import { StatusPill } from "@/components/StatusPill";
import { formatCurrency } from "@/lib/mock/data";
import { appRoutes } from "@/lib/routes";
import { getExpert, getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function ExpertDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  
  // Fetch expert details
  const expert = await getExpert(MOCK_AUTH_TOKEN, id, {
    next: { revalidate: 3600, tags: [cacheTags.expert(id)] },
  }).catch(() => null);

  // Fetch expert slots (no-cache to ensure real-time availability)
  const expertSlots = await getExpertSlots(MOCK_AUTH_TOKEN, id, {
    next: { revalidate: 0, tags: [cacheTags.expertSlots(id)] },
  }).catch(() => []);

  if (!expert) {
    return (
      <AppShell>
        <PageHeader eyebrow="Error" title="Expert not found" />
      </AppShell>
    );
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow={expert.specialty?.name}
        title={expert.name}
        description={expert.bio ?? "Expert consultation slots are shown below with live status support."}
        action={
          <Link
            href={appRoutes.experts}
            className="rounded-md border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700"
          >
            Back to experts
          </Link>
        }
      />

      <section className="mb-8 grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-lg border border-slate-100 bg-white p-6 shadow-sm">
          <div className="mb-5 flex flex-wrap items-center gap-2">
            <StatusPill status={expert.specialty?.name ?? "General"} />
            {expert.tags && (
              <span className="rounded-md bg-violet-50 px-2.5 py-1 text-xs font-bold text-violet-700 ring-1 ring-violet-100">
                {expert.tags}
              </span>
            )}
          </div>
          <h2 className="mb-2 text-xl font-black text-slate-950">{expert.title}</h2>
          <p className="leading-7 text-slate-600">
            Session price is {formatCurrency(expert.sessionPrice, expert.currency)}. Slot locking reserves
            one appointment for five minutes while payment is completed.
          </p>
        </div>
        <div className="rounded-lg border border-cyan-100 bg-cyan-50 p-6 shadow-sm">
          <p className="text-sm font-bold uppercase tracking-[0.16em] text-cyan-700">Realtime status</p>
          <p className="mt-3 text-3xl font-black text-slate-950">{expertSlots.length} slots</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Available slots turn green, held slots turn amber, and booked slots turn grey.
          </p>
        </div>
      </section>

      <SlotGrid expertId={expert.id} slots={expertSlots} />
    </AppShell>
  );
}
