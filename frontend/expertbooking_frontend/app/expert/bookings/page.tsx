import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatCurrency, formatDateTime } from "@/lib/mock/data"; // Used for formatting
import { getExpertBookings } from "@/lib/api/expert-account";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";

export default async function ExpertBookingsPage() {
  const token = await getServerAuthToken();
  const expertBookings = await getExpertBookings(token, {
    next: { revalidate: 0, tags: [cacheTags.expertBookings("all")] },
  }).catch(() => []);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Expert"
        title="Patient bookings"
        description="All confirmed patient sessions for the signed-in expert account."
      />

      <section className="grid gap-4">
        {expertBookings.length === 0 ? (
          <p className="text-slate-500 text-sm">No bookings found.</p>
        ) : (
          expertBookings.map((booking) => (
            <article key={booking.bookingId} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <div className="mb-2 flex flex-wrap items-center gap-2">
                    <h2 className="text-lg font-black text-slate-950">{booking.patientName || "Unknown Patient"}</h2>
                    <StatusPill status={booking.status} />
                  </div>
                  <p className="text-sm text-slate-500">{booking.patientEmail}</p>
                  <p className="mt-1 text-sm text-slate-500">{formatDateTime(booking.startTime)}</p>
                </div>
                <span className="rounded-md bg-cyan-50 px-3 py-2 text-sm font-black text-cyan-700">
                  {formatCurrency(booking.amountPaid, booking.currency)}
                </span>
              </div>
            </article>
          ))
        )}
      </section>
    </AppShell>
  );
}
