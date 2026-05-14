import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatusPill } from "@/components/StatusPill";
import { formatDateTime } from "@/lib/mock/data"; // Only used for date formatting
import { getAdminExperts } from "@/lib/api/admin";
import { getExpertSlots } from "@/lib/api/experts";
import { cacheTags } from "@/lib/api/cache-keys";
import type { TimeSlotDto } from "@/types/api";
import { getServerAuthToken } from "@/lib/auth";
import { SlotForm } from "@/components/admin/SlotForm";
import { SlotDeleteButton } from "@/components/admin/SlotDeleteButton";

export default async function AdminSlotsPage() {
  const token = await getServerAuthToken();
  // Fetch all experts first since there's no global "get all slots" endpoint
  const experts = await getAdminExperts(token, {
    next: { revalidate: 0, tags: [cacheTags.adminExperts] },
  }).catch(() => []);

  // Fetch slots for all experts
  const slotPromises = experts.map((expert) =>
    getExpertSlots(token, expert.id, {
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

      <SlotForm experts={experts} />

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
                <div className="flex flex-col items-end gap-2">
                  <StatusPill status={slot.status} />
                  {slot.status === "AVAILABLE" && (
                    <SlotDeleteButton id={slot.id} expertId={slot.expertId} />
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
