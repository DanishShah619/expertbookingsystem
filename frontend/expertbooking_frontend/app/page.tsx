import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { ExpertCard } from "@/components/ExpertCard";
import { getExperts, getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import { appRoutes } from "@/lib/routes";
import { getServerAuthToken } from "@/lib/auth";
import { getCurrentAuthUser } from "@/lib/api/auth";

export default async function Home() {
  const token = await getServerAuthToken();
  const user = token ? await getCurrentAuthUser(token, { next: { revalidate: 0 } }).catch(() => null) : null;
  const experts = await getExperts(token, {}, {
    next: { revalidate: 3600, tags: [cacheTags.experts] },
  }).catch(() => []);

  // Fetch slots for all experts to calculate available count (parallel)
  const slotPromises = experts.map((expert) =>
    getExpertSlots(token, expert.id, {
      next: { revalidate: 60, tags: [cacheTags.expertSlots(expert.id)] },
    }).catch(() => [])
  );
  
  const allSlotsArrays = await Promise.all(slotPromises);
  const availableSlots = allSlotsArrays.flat().filter((slot) => slot.status === "AVAILABLE").length;
  const primaryHref = user ? appRoutes.dashboard : appRoutes.login;

  return (
    <AppShell>
      <section className="grid min-h-[560px] items-center gap-10 py-8 lg:grid-cols-[1.05fr_0.95fr] lg:py-14">
        <div>
          <p className="mb-5 inline-flex rounded-md border border-cyan-200 bg-cyan-50 px-3 py-1 text-sm font-bold text-cyan-800">
            Public expert discovery
          </p>
          <h1 className="max-w-3xl text-5xl font-black leading-[1.05] text-slate-950 sm:text-6xl">
            Find the right expert before you sign in.
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
            Browse specialists, compare prices, and review available slots freely. When it is time to reserve a
            session, sign in with Google and continue straight to booking.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              href={primaryHref}
              className="inline-flex items-center justify-center rounded-md bg-slate-950 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-cyan-700"
            >
              {user ? "Open dashboard" : "Sign in"}
            </Link>
            <Link
              href={appRoutes.experts}
              className="inline-flex items-center justify-center rounded-md border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-700 shadow-sm transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700"
            >
              Browse experts
            </Link>
          </div>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid gap-3 sm:grid-cols-3">
            <LandingMetric label="Experts" value={experts.length} />
            <LandingMetric label="Open slots" value={availableSlots} />
            <LandingMetric label="Booking lock" value="5 min" />
          </div>
          <div className="mt-5 rounded-lg bg-slate-950 p-5 text-white">
            <p className="text-sm font-bold text-cyan-200">Booking rule</p>
            <p className="mt-2 text-2xl font-black">Guests can browse. Signed-in users can book.</p>
          </div>
        </div>
      </section>

      <section className="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-bold uppercase text-cyan-700">Featured experts</p>
          <h2 className="mt-2 text-3xl font-black text-slate-950">Start with available specialists</h2>
        </div>
        <Link
          href={appRoutes.experts}
          className="inline-flex items-center justify-center rounded-md border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700"
        >
          View all experts
        </Link>
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        {experts.length === 0 ? (
          <div className="rounded-lg border border-slate-200 bg-white p-6 text-slate-600 shadow-sm">
            No experts are available yet.
          </div>
        ) : (
          experts.slice(0, 3).map((expert) => <ExpertCard key={expert.id} expert={expert} />)
        )}
      </section>
    </AppShell>
  );
}

function LandingMetric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-lg border border-slate-100 bg-slate-50 p-4">
      <p className="text-sm font-bold text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-black text-slate-950">{value}</p>
    </div>
  );
}
