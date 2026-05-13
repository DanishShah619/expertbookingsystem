import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime, slots } from "@/lib/mock/data";

export default function AdminSlotsPage() {
  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage slots"
        description="Create future slots for experts and delete only those still available."
      />

      <section className="mb-6 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
        <h2 className="mb-4 text-lg font-black text-slate-950">Create slot</h2>
        <div className="grid gap-3 md:grid-cols-3">
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="Expert ID" />
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="Start time" />
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="End time" />
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        {slots.map((slot) => (
          <article key={slot.id} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="font-black text-slate-950">{formatDateTime(slot.startTime)}</p>
                <p className="mt-1 text-sm text-slate-500">Expert ID {slot.expertId}</p>
              </div>
              <StatusPill status={slot.status} />
            </div>
          </article>
        ))}
      </section>
    </AppShell>
  );
}
