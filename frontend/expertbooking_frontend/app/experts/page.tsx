import { AppShell } from "@/components/AppShell";
import { ExpertCard } from "@/components/ExpertCard";
import { PageHeader } from "@/components/PageHeader";
import { ExpertSearchBar } from "@/components/ExpertSearchBar";
import { getExperts, getSpecialties } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";

export default async function ExpertsPage({
  searchParams,
}: {
  searchParams: Promise<{ search?: string; specialty?: string }>;
}) {
  const resolvedParams = await searchParams;
  const token = await getServerAuthToken();

  const [experts, specialties] = await Promise.all([
    getExperts(token, resolvedParams, {
      next: { revalidate: 3600, tags: [cacheTags.experts] },
    }).catch(() => []),
    getSpecialties(token, {
      next: { revalidate: 3600, tags: [cacheTags.specialties] },
    }).catch(() => []),
  ]);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Experts"
        title="Browse doctors by specialty and availability"
        description="Filters and search are laid out for the backend /api/experts query params. Live data can drop into this page without changing the card layout."
      />

      <ExpertSearchBar specialties={specialties} />

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {experts.length === 0 ? (
          <p className="text-slate-500 col-span-full">No experts found matching your criteria.</p>
        ) : (
          experts.map((expert) => (
            <ExpertCard key={expert.id} expert={expert} />
          ))
        )}
      </section>
    </AppShell>
  );
}
