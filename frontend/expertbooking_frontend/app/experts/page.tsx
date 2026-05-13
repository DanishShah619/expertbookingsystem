import { AppShell } from "@/components/AppShell";
import { ExpertCard } from "@/components/ExpertCard";
import { PageHeader } from "@/components/PageHeader";
import { experts, specialties } from "@/lib/mock/data";

export default function ExpertsPage() {
  return (
    <AppShell>
      <PageHeader
        eyebrow="Experts"
        title="Browse doctors by specialty and availability"
        description="Filters and search are laid out for the backend /api/experts query params. Live data can drop into this page without changing the card layout."
      />

      <section className="mb-6 grid gap-3 rounded-lg border border-slate-100 bg-white p-4 shadow-sm lg:grid-cols-[1fr_220px]">
        <input
          className="rounded-md border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50"
          placeholder="Search by expert name, specialty, or tags"
        />
        <select className="rounded-md border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50">
          <option value="">All specialties</option>
          {specialties.map((specialty) => (
            <option key={specialty.id} value={specialty.slug}>
              {specialty.name}
            </option>
          ))}
        </select>
      </section>

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {experts.map((expert) => (
          <ExpertCard key={expert.id} expert={expert} />
        ))}
      </section>
    </AppShell>
  );
}
