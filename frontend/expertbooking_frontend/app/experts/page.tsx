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

  const [expertsResult, specialtiesResult] = await Promise.allSettled([
    getExperts(token, resolvedParams, {
      next: { revalidate: 3600, tags: [cacheTags.experts] },
    }),
    getSpecialties(token, {
      next: { revalidate: 3600, tags: [cacheTags.specialties] },
    }),
  ]);
  const experts = expertsResult.status === "fulfilled" ? expertsResult.value : [];
  const specialties = specialtiesResult.status === "fulfilled" ? specialtiesResult.value : [];
  const loadError =
    expertsResult.status === "rejected"
      ? "Experts could not be loaded right now. Please try again in a moment."
      : null;

  return (
    <AppShell>
      <PageHeader
        eyebrow="Experts"
        title="Browse doctors by specialty and availability"
        description="Filters and search are laid out for the backend /api/experts query params. Live data can drop into this page without changing the card layout."
      />

      <ExpertSearchBar specialties={specialties} />

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {loadError ? (
          <p className="col-span-full rounded-lg border border-rose-100 bg-rose-50 p-4 text-sm font-semibold text-rose-700">
            {loadError}
          </p>
        ) : experts.length === 0 ? (
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
