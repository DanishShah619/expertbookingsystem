import Link from "next/link";
import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatCurrency, formatDateTime } from "@/lib/mock/data"; // Used for formatting only
import { getUserBookings, getUserUpcomingBookings, getUserPastBookings } from "@/lib/api/user";
import { cacheTags } from "@/lib/api/cache-keys";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function MyBookingsPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>;
}) {
  const { tab } = await searchParams;
  const currentTab = tab === "upcoming" || tab === "past" ? tab : "all";

  let bookings = [];

  const fetchOptions = {
    next: { revalidate: 0, tags: [cacheTags.userBookings()] },
  };

  if (currentTab === "upcoming") {
    bookings = await getUserUpcomingBookings(MOCK_AUTH_TOKEN, fetchOptions).catch(() => []);
  } else if (currentTab === "past") {
    bookings = await getUserPastBookings(MOCK_AUTH_TOKEN, fetchOptions).catch(() => []);
  } else {
    bookings = await getUserBookings(MOCK_AUTH_TOKEN, fetchOptions).catch(() => []);
  }

  const tabs = [
    { label: "All", value: "all" },
    { label: "Upcoming", value: "upcoming" },
    { label: "Past", value: "past" },
  ];

  return (
    <AppShell>
      <PageHeader
        eyebrow="My bookings"
        title="All consultations"
        description="Tabs are prepared for all, upcoming, and past booking endpoints."
      />

      <div className="mb-5 flex flex-wrap gap-2">
        {tabs.map((t) => {
          const isActive = currentTab === t.value;
          return (
            <Link
              key={t.value}
              href={`?tab=${t.value}`}
              className={`rounded-md border px-4 py-2 text-sm font-bold transition ${
                isActive
                  ? "border-cyan-300 bg-cyan-50 text-cyan-700"
                  : "border-slate-200 bg-white text-slate-700 hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700"
              }`}
            >
              {t.label}
            </Link>
          );
        })}
      </div>

      <section className="grid gap-4">
        {bookings.length === 0 ? (
          <p className="text-slate-500 text-sm">No bookings found for this category.</p>
        ) : (
          bookings.map((booking) => (
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
                  {booking.status === "CONFIRMED" && (
                    <button
                      type="button"
                      className="rounded-md border border-rose-200 bg-white px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))
        )}
      </section>
    </AppShell>
  );
}
