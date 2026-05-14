import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { getAdminSpecialties } from "@/lib/api/admin";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";
import { SpecialtyForm } from "@/components/admin/SpecialtyForm";
import { SpecialtyDeleteButton } from "@/components/admin/SpecialtyDeleteButton";

export default async function AdminSpecialtiesPage() {
  const token = await getServerAuthToken();
  const specialties = await getAdminSpecialties(token, {
    next: { revalidate: 0, tags: [cacheTags.adminSpecialties] },
  }).catch(() => []);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage specialties"
        description="Specialty names generate slugs on the backend and drive expert filters."
      />

      <SpecialtyForm />

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {specialties.length === 0 ? (
          <p className="text-slate-500 text-sm">No specialties found.</p>
        ) : (
          specialties.map((specialty) => (
            <article key={specialty.id} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm flex items-start justify-between gap-4">
              <div>
                <p className="font-black text-slate-950">{specialty.name}</p>
                <p className="mt-1 text-sm font-semibold text-cyan-600">{specialty.slug}</p>
              </div>
              <SpecialtyDeleteButton id={specialty.id} />
            </article>
          ))
        )}
      </section>
    </AppShell>
  );
}
