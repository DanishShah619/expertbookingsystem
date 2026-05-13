import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatCurrency, formatDateTime, userBookings } from "@/lib/mock/data";

export default function MyBookingsPage() {
  return (
    <AppShell>
      <PageHeader
        eyebrow="My bookings"
        title="All consultations"
        description="Tabs are prepared for all, upcoming, and past booking endpoints."
      />

      <div className="mb-5 flex flex-wrap gap-2">
        {["All", "Upcoming", "Past"].map((tab) => (
          <button
            key={tab}
            type="button"
            className="rounded-md border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700"
          >
            {tab}
          </button>
        ))}
      </div>

      <section className="grid gap-4">
        {userBookings.map((booking) => (
          <article key={booking.id} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-black text-slate-950">{booking.expertName}</h2>
                  <StatusPill status={booking.status} />
                </div>
                <p className="text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-md bg-emerald-50 px-3 py-2 text-sm font-black text-emerald-700">
                  {formatCurrency(booking.amountPaid, booking.currency)}
                </span>
                <button
                  type="button"
                  className="rounded-md border border-rose-200 bg-white px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          </article>
        ))}
      </section>
    </AppShell>
  );
}
