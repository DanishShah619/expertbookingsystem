import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatTime } from "@/lib/mock/data"; // Only used for time formatting
import { getExpertTodayBookings } from "@/lib/api/expert-account";
import { cacheTags } from "@/lib/api/cache-keys";
import { getServerAuthToken } from "@/lib/auth";

export default async function ExpertSchedulePage() {
  const token = await getServerAuthToken();
  const expertBookings = await getExpertTodayBookings(token, {
    next: { revalidate: 0, tags: [cacheTags.expertBookings("today")] },
  }).catch(() => []);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Schedule"
        title="Today's sessions"
        description="A simple day view for the expert's confirmed appointments."
      />

      <section className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
        <div className="relative space-y-4">
          {expertBookings.length === 0 ? (
            <p className="text-slate-500 text-sm">No sessions scheduled for today.</p>
          ) : (
            expertBookings.map((booking) => (
              <article key={booking.bookingId} className="grid gap-3 rounded-lg border border-slate-100 bg-[#fbfdff] p-4 md:grid-cols-[120px_1fr_120px] md:items-center">
                <p className="text-xl font-black text-cyan-700">{formatTime(booking.startTime)}</p>
                <div>
                  <p className="font-black text-slate-950">{booking.patientName}</p>
                  <p className="text-sm text-slate-500">{booking.patientEmail}</p>
                </div>
                <StatusPill status={booking.isToday ? "CONFIRMED" : "BOOKED"} />
              </article>
            ))
          )}
        </div>
      </section>
    </AppShell>
  );
}
