import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatCurrency } from "@/lib/mock/data"; // Only used for currency formatting
import { getAdminExperts, getAdminSpecialties } from "@/lib/api/admin";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";
import { ExpertForm } from "@/components/admin/ExpertForm";
import { ExpertDeleteButton } from "@/components/admin/ExpertDeleteButton";

export default async function AdminExpertsPage() {
  const token = await getServerAuthToken();
  const [experts, specialties] = await Promise.all([
    getAdminExperts(token, {
      next: { revalidate: 0, tags: [cacheTags.adminExperts] },
    }).catch(() => []),
    getAdminSpecialties(token, {
      next: { revalidate: 0, tags: [cacheTags.adminSpecialties] },
    }).catch(() => []),
  ]);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage experts"
        description="Create and update expert profiles, linked user accounts, specialties, and session prices."
      />

      <ExpertForm specialties={specialties} />

      <section className="overflow-hidden rounded-lg border border-slate-100 bg-white shadow-sm">
        {experts.length === 0 ? (
          <div className="p-5 text-slate-500 text-sm">No experts found.</div>
        ) : (
          experts.map((expert) => (
            <div key={expert.id} className="grid gap-3 border-b border-slate-100 p-5 last:border-b-0 lg:grid-cols-[1fr_160px_140px_140px] lg:items-center">
              <div>
                <p className="font-black text-slate-950">{expert.name}</p>
                <p className="text-sm text-slate-500">{expert.title}</p>
              </div>
              <StatusPill status={expert.specialty?.name ?? "General"} />
              <p className="font-black text-slate-950">{formatCurrency(expert.sessionPrice, expert.currency)}</p>
              <ExpertDeleteButton id={expert.id} />
            </div>
          ))
        )}
      </section>
    </AppShell>
  );
}
