import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime } from "@/lib/mock/data"; // Only used for date formatting
import { appRoutes } from "@/lib/routes";
import { getExpertProfile, getExpertUpcomingBookings } from "@/lib/api/expert-account";
import { cacheTags } from "@/lib/api/cache-keys";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function ExpertDashboardPage() {
  const expertProfile = await getExpertProfile(MOCK_AUTH_TOKEN, {
    next: { revalidate: 0, tags: [cacheTags.expertProfile()] },
  }).catch(() => null);

  const upcomingBookings = await getExpertUpcomingBookings(MOCK_AUTH_TOKEN, {
    next: { revalidate: 0, tags: [cacheTags.expertBookings("upcoming")] },
  }).catch(() => []);

  if (!expertProfile) {
    return (
      <AppShell>
        <PageHeader eyebrow="Error" title="Expert profile not found" />
      </AppShell>
    );
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Expert dashboard"
        title={`Welcome, ${expertProfile.name}`}
        description="A focused view of upcoming consultations, patient bookings, and today's schedule."
        action={
          <Link
            href={appRoutes.expertSchedule}
            className="rounded-md bg-cyan-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-cyan-700"
          >
            View schedule
          </Link>
        }
      />

      <section className="mb-8 grid gap-4 sm:grid-cols-3">
        <StatCard label="Total bookings" value={expertProfile.totalBookings} tone="cyan" />
        <StatCard label="Upcoming" value={expertProfile.upcomingBookings} tone="emerald" />
        <StatCard label="Completed" value={expertProfile.completedBookings} tone="violet" />
      </section>

      <section className="rounded-lg border border-slate-100 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5">
          <h2 className="text-lg font-black text-slate-950">Next sessions</h2>
        </div>
        {upcomingBookings.length === 0 ? (
          <div className="p-5 text-slate-500 text-sm">No upcoming sessions.</div>
        ) : (
          upcomingBookings.slice(0, 5).map((booking) => (
            <div key={booking.bookingId} className="flex flex-col gap-3 border-b border-slate-100 p-5 last:border-b-0 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-black text-slate-950">{booking.patientName}</p>
                <p className="text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
              </div>
              <StatusPill status={booking.status} />
            </div>
          ))
        )}
      </section>
    </AppShell>
  );
}
