import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime, userBookings } from "@/lib/mock/data";
import { appRoutes } from "@/lib/routes";

export default function DashboardPage() {
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
        <StatCard label="Total" value={userBookings.length} tone="cyan" />
        <StatCard label="Upcoming" value={1} tone="emerald" />
        <StatCard label="Completed" value={1} tone="violet" />
        <StatCard label="Cancelled" value={0} tone="amber" />
      </section>

      <section className="rounded-lg border border-slate-100 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5">
          <h2 className="text-lg font-black text-slate-950">Recent bookings</h2>
        </div>
        <div className="divide-y divide-slate-100">
          {userBookings.map((booking) => (
            <div key={booking.id} className="flex flex-col gap-3 p-5 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-bold text-slate-950">{booking.expertName}</p>
                <p className="text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
              </div>
              <StatusPill status={booking.status} />
            </div>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
