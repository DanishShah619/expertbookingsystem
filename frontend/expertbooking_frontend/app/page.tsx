import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { ExpertCard } from "@/components/ExpertCard";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { getExperts, getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import { appRoutes } from "@/lib/routes";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function Home() {
  const experts = await getExperts(MOCK_AUTH_TOKEN, {}, {
    next: { revalidate: 3600, tags: [cacheTags.experts] },
  }).catch(() => []);

  // Fetch slots for all experts to calculate available count (parallel)
  const slotPromises = experts.map((expert) =>
    getExpertSlots(MOCK_AUTH_TOKEN, expert.id, {
      next: { revalidate: 60, tags: [cacheTags.expertSlots(expert.id)] },
    }).catch(() => [])
  );
  
  const allSlotsArrays = await Promise.all(slotPromises);
  const availableSlots = allSlotsArrays.flat().filter((slot) => slot.status === "AVAILABLE").length;

  return (
    <AppShell>
      <PageHeader
        eyebrow="Booking workspace"
        title="Find an expert, reserve a live slot, and complete payment in one flow."
        description="This first frontend pass is wired around the backend endpoint contract and ready for Google auth, Stripe checkout, WebSocket slot updates, and later cache integration."
        action={
          <Link
            href={appRoutes.experts}
            className="rounded-md bg-cyan-600 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-cyan-700"
          >
            Browse experts
          </Link>
        }
      />

      <section className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Experts" value={experts.length} tone="cyan" />
        <StatCard label="Available slots" value={availableSlots} tone="emerald" />
        <StatCard label="Seat lock" value="5 min" tone="amber" />
        <StatCard label="Payment" value="Stripe" tone="violet" />
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        {experts.slice(0, 3).map((expert) => (
          <ExpertCard key={expert.id} expert={expert} />
        ))}
      </section>
    </AppShell>
  );
}
