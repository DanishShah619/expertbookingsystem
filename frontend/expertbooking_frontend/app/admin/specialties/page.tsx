import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { getAdminSpecialties } from "@/lib/api/admin";
import { cacheTags } from "@/lib/api/cache-keys";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function AdminSpecialtiesPage() {
  const specialties = await getAdminSpecialties(MOCK_AUTH_TOKEN, {
    next: { revalidate: 0, tags: [cacheTags.adminSpecialties] },
  }).catch(() => []);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage specialties"
        description="Specialty names generate slugs on the backend and drive expert filters."
      />

      <section className="mb-6 flex flex-col gap-3 rounded-lg border border-slate-100 bg-white p-5 shadow-sm sm:flex-row">
        <input
          className="min-w-0 flex-1 rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-50"
          placeholder="Specialty name"
        />
        <button className="rounded-md bg-emerald-500 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-600">
          Add specialty
        </button>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {specialties.length === 0 ? (
          <p className="text-slate-500 text-sm">No specialties found.</p>
        ) : (
          specialties.map((specialty) => (
            <article key={specialty.id} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
              <p className="font-black text-slate-950">{specialty.name}</p>
              <p className="mt-1 text-sm font-semibold text-cyan-600">{specialty.slug}</p>
            </article>
          ))
        )}
      </section>
    </AppShell>
  );
}
