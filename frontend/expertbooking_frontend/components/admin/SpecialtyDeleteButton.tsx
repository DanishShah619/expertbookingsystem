"use client";

import { deleteSpecialtyAction } from "@/app/actions/admin";
import { useTransition } from "react";

export function SpecialtyDeleteButton({ id }: { id: number }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      onClick={() => {
        if (confirm("Are you sure you want to delete this specialty?")) {
          startTransition(() => {
            deleteSpecialtyAction(id);
          });
        }
      }}
      disabled={isPending}
      className="rounded-md border border-rose-200 px-3 py-1.5 text-xs font-bold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50"
    >
      {isPending ? "Deleting..." : "Delete"}
    </button>
  );
}
