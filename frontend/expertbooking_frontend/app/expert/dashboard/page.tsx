import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { expertBookings, experts, formatDateTime } from "@/lib/mock/data";
import { appRoutes } from "@/lib/routes";

export default function ExpertDashboardPage() {
  const expert = experts[0];

  return (
    <AppShell>
      <PageHeader
        eyebrow="Expert dashboard"
        title={`Welcome, ${expert.name}`}
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
        <StatCard label="Total bookings" value={expertBookings.length} tone="cyan" />
        <StatCard label="Upcoming" value={2} tone="emerald" />
        <StatCard label="Completed" value={8} tone="violet" />
      </section>

      <section className="rounded-lg border border-slate-100 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5">
          <h2 className="text-lg font-black text-slate-950">Next sessions</h2>
        </div>
        {expertBookings.map((booking) => (
          <div key={booking.bookingId} className="flex flex-col gap-3 border-b border-slate-100 p-5 last:border-b-0 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="font-black text-slate-950">{booking.patientName}</p>
              <p className="text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
            </div>
            <StatusPill status={booking.status} />
          </div>
        ))}
      </section>
    </AppShell>
  );
}
