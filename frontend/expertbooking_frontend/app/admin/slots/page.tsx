import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime } from "@/lib/mock/data"; // Only used for date formatting
import { getAdminExperts } from "@/lib/api/admin";
import { getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import type { TimeSlotDto } from "@/types/api";

const MOCK_AUTH_TOKEN = "MOCK_TOKEN";

export default async function AdminSlotsPage() {
  // Fetch all experts first since there's no global "get all slots" endpoint
  const experts = await getAdminExperts(MOCK_AUTH_TOKEN, {
    next: { revalidate: 0, tags: [cacheTags.adminExperts] },
  }).catch(() => []);

  // Fetch slots for all experts
  const slotPromises = experts.map((expert) =>
    getExpertSlots(MOCK_AUTH_TOKEN, expert.id, {
      next: { revalidate: 0, tags: [cacheTags.expertSlots(expert.id)] },
    }).catch(() => [])
  );

  const allSlotsArrays = await Promise.all(slotPromises);
  const slots: TimeSlotDto[] = allSlotsArrays.flat();

  return (
    <AppShell>
      <PageHeader
        eyebrow="Admin"
        title="Manage slots"
        description="Create future slots for experts and delete only those still available."
      />

      <section className="mb-6 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
        <h2 className="mb-4 text-lg font-black text-slate-950">Create slot</h2>
        <div className="grid gap-3 md:grid-cols-3">
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="Expert ID" />
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="Start time" />
          <input className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50" placeholder="End time" />
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        {slots.length === 0 ? (
          <p className="text-slate-500 text-sm">No slots found across any experts.</p>
        ) : (
          slots.map((slot) => (
            <article key={slot.id} className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-black text-slate-950">{formatDateTime(slot.startTime)}</p>
                  <p className="mt-1 text-sm text-slate-500">Expert ID {slot.expertId}</p>
                </div>
                <StatusPill status={slot.status} />
              </div>
            </article>
          ))
        )}
      </section>
    </AppShell>
  );
}
