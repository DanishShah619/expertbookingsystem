"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import { addExpertAction } from "@/app/actions/admin";
import { useEffect, useRef } from "react";
import type { SpecialtyDto } from "@/types/api";

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button
      type="submit"
      disabled={pending}
      className="col-span-full mt-2 rounded-md bg-violet-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-violet-700 disabled:opacity-50"
    >
      {pending ? "Saving..." : "Save Expert"}
    </button>
  );
}

export function ExpertForm({ specialties }: { specialties: SpecialtyDto[] }) {
  const [state, formAction] = useActionState(addExpertAction, { message: null, error: null });
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.message) {
      formRef.current?.reset();
    }
  }, [state.message]);

  return (
    <form ref={formRef} action={formAction} className="mb-6 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
      <h2 className="mb-4 text-lg font-black text-slate-950">Add New Expert</h2>
      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <input
          name="userId"
          type="number"
          required
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-violet-400 focus:ring-4 focus:ring-violet-50"
          placeholder="Linked User ID"
        />
        <input
          name="name"
          required
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-violet-400 focus:ring-4 focus:ring-violet-50"
          placeholder="Expert Name"
        />
        <select
          name="specialtyId"
          required
          title="Specialty"
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-violet-400 focus:ring-4 focus:ring-violet-50 bg-white"
        >
          <option value="">Select Specialty</option>
          {specialties.map((spec) => (
            <option key={spec.id} value={spec.id}>
              {spec.name}
            </option>
          ))}
        </select>
        <input
          name="sessionPrice"
          type="number"
          required
          className="rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-violet-400 focus:ring-4 focus:ring-violet-50"
          placeholder="Price (INR)"
        />
        <SubmitButton />
      </div>
      {state.error && <p className="mt-4 text-sm text-rose-500 font-medium">{state.error}</p>}
      {state.message && <p className="mt-4 text-sm text-emerald-600 font-medium">{state.message}</p>}
    </form>
  );
}
