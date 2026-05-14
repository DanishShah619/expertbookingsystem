"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import { addSlotAction } from "@/app/actions/admin";
import { useEffect, useRef } from "react";
import type { ExpertDto } from "@/types/api";

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button
      type="submit"
      disabled={pending}
      className="col-span-full mt-2 rounded-md bg-amber-500 px-5 py-3 text-sm font-bold text-white transition hover:bg-amber-600 disabled:opacity-50"
    >
      {pending ? "Adding..." : "Add Slot"}
    </button>
  );
}

export function SlotForm({ experts }: { experts: ExpertDto[] }) {
  const [state, formAction] = useActionState(addSlotAction, { message: null, error: null });
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.message) {
      formRef.current?.reset();
    }
  }, [state.message]);

  return (
    <form ref={formRef} action={formAction} className="mb-6 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
      <h2 className="mb-4 text-lg font-black text-slate-950">Add New Slot</h2>
      <div className="grid gap-3 md:grid-cols-3">
        <select
          name="expertId"
          required
          title="Expert"
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-amber-400 focus:ring-4 focus:ring-amber-50 bg-white"
        >
          <option value="">Select Expert</option>
          {experts.map((exp) => (
            <option key={exp.id} value={exp.id}>
              {exp.name} ({exp.specialty?.name})
            </option>
          ))}
        </select>
        <input
          name="startTime"
          type="datetime-local"
          required
          title="Start Time"
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-amber-400 focus:ring-4 focus:ring-amber-50"
        />
        <input
          name="endTime"
          type="datetime-local"
          required
          title="End Time"
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-amber-400 focus:ring-4 focus:ring-amber-50"
        />
        <SubmitButton />
      </div>
      {state.error && <p className="mt-4 text-sm text-rose-500 font-medium">{state.error}</p>}
      {state.message && <p className="mt-4 text-sm text-emerald-600 font-medium">{state.message}</p>}
    </form>
  );
}
