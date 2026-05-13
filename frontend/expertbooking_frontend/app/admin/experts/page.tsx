import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { experts, formatCurrency } from "@/lib/mock/data";

export default function AdminExpertsPage() {
  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage experts"
        description="Create and update expert profiles, linked user accounts, specialties, and session prices."
        action={
          <button className="rounded-md bg-violet-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-violet-700">
            New expert
          </button>
        }
      />

      <section className="mb-6 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
        <h2 className="mb-4 text-lg font-black text-slate-950">Expert form</h2>
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
          {["User ID", "Name", "Specialty ID", "Price INR"].map((label) => (
            <input
              key={label}
              className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-violet-400 focus:ring-4 focus:ring-violet-50"
              placeholder={label}
            />
          ))}
        </div>
      </section>

      <section className="overflow-hidden rounded-lg border border-slate-100 bg-white shadow-sm">
        {experts.map((expert) => (
          <div key={expert.id} className="grid gap-3 border-b border-slate-100 p-5 last:border-b-0 lg:grid-cols-[1fr_160px_140px_140px] lg:items-center">
            <div>
              <p className="font-black text-slate-950">{expert.name}</p>
              <p className="text-sm text-slate-500">{expert.title}</p>
            </div>
            <StatusPill status={expert.specialty?.name ?? "General"} />
            <p className="font-black text-slate-950">{formatCurrency(expert.sessionPrice, expert.currency)}</p>
            <button className="rounded-md border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 hover:bg-slate-50">
              Edit
            </button>
          </div>
        ))}
      </section>
    </AppShell>
  );
}
