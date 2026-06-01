import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime } from "@/lib/mock/data"; // Used for date formatting only
import { appRoutes } from "@/lib/routes";
import { getUserProfile, getUserBookings } from "@/lib/api/user";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";

// Force Next.js to always re-render this page on each navigation request.
// Without this, the router cache serves a stale RSC payload even after
// revalidateTag() has cleared the underlying fetch cache.
export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const token = await getServerAuthToken();
  const profile = await getUserProfile(token, {
    next: { revalidate: 0, tags: [cacheTags.userProfile()] },
  }).catch(() => null);

  const bookings = await getUserBookings(token, {
    next: { revalidate: 0, tags: [cacheTags.userBookings()] },
  }).catch(() => []);

  // Fallback if profile fails
  const stats = profile || {
    totalBookings: 0,
    upcomingBookings: 0,
    completedBookings: 0,
    cancelledBookings: 0,
  };

  return (
    <AppShell>
      <PageHeader
        eyebrow="User dashboard"
        title="Your booking overview"
        description="A compact dashboard for current sessions, past sessions, and cancellation visibility."
        action={
          <Link
            href={appRoutes.experts}
            className="rounded-md bg-cyan-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-cyan-700"
          >
            Book a session
          </Link>
        }
      />

      <section className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total" value={stats.totalBookings} tone="cyan" />
        <StatCard label="Upcoming" value={stats.upcomingBookings} tone="emerald" />
        <StatCard label="Completed" value={stats.completedBookings} tone="violet" />
        <StatCard label="Cancelled" value={stats.cancelledBookings} tone="amber" />
      </section>

      <section className="rounded-lg border border-slate-100 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5">
          <h2 className="text-lg font-black text-slate-950">Recent bookings</h2>
        </div>
        <div className="divide-y divide-slate-100">
          {bookings.length === 0 ? (
            <div className="p-5 text-slate-500 text-sm">No recent bookings.</div>
          ) : (
            bookings.slice(0, 5).map((booking) => (
              <div key={booking.id} className="flex flex-col gap-3 p-5 md:flex-row md:items-center md:justify-between">
                <div>
                  <p className="font-bold text-slate-950">{booking.expertName}</p>
                  <p className="text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
                </div>
                <StatusPill status={booking.status} />
              </div>
            ))
          )}
        </div>
      </section>
    </AppShell>
  );
}
