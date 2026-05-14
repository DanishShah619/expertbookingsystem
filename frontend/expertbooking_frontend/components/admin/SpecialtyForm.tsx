"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import { addSpecialtyAction } from "@/app/actions/admin";
import { useEffect, useRef } from "react";

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button
      type="submit"
      disabled={pending}
      className="rounded-md bg-emerald-500 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-600 disabled:opacity-50"
    >
      {pending ? "Adding..." : "Add specialty"}
    </button>
  );
}

export function SpecialtyForm() {
  const [state, formAction] = useActionState(addSpecialtyAction, { message: null, error: null });
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.message) {
      formRef.current?.reset();
    }
  }, [state.message]);

  return (
    <form ref={formRef} action={formAction} className="mb-6 flex flex-col gap-3 rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row">
        <input
          name="name"
          required
          className="min-w-0 flex-1 rounded-md border border-slate-200 px-4 py-3 text-sm outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-50"
          placeholder="Specialty name"
        />
        <SubmitButton />
      </div>
      {state.error && <p className="text-sm text-rose-500 font-medium">{state.error}</p>}
      {state.message && <p className="text-sm text-emerald-600 font-medium">{state.message}</p>}
    </form>
  );
}
