import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { expertBookings, formatTime } from "@/lib/mock/data";

export default function ExpertSchedulePage() {
  return (
    <AppShell>
      <PageHeader
        eyebrow="Schedule"
        title="Today's sessions"
        description="A simple day view for the expert's confirmed appointments."
      />

      <section className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
        <div className="relative space-y-4">
          {expertBookings.map((booking) => (
            <article key={booking.bookingId} className="grid gap-3 rounded-lg border border-slate-100 bg-[#fbfdff] p-4 md:grid-cols-[120px_1fr_120px] md:items-center">
              <p className="text-xl font-black text-cyan-700">{formatTime(booking.startTime)}</p>
              <div>
                <p className="font-black text-slate-950">{booking.patientName}</p>
                <p className="text-sm text-slate-500">{booking.patientEmail}</p>
              </div>
              <StatusPill status={booking.today ? "CONFIRMED" : "BOOKED"} />
            </article>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
