"use client";

import { deleteExpertAction } from "@/app/actions/admin";
import { useTransition } from "react";

export function ExpertDeleteButton({ id }: { id: number }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      onClick={() => {
        if (confirm("Are you sure you want to delete this expert?")) {
          startTransition(() => {
            deleteExpertAction(id);
          });
        }
      }}
      disabled={isPending}
      className="rounded-md border border-rose-200 px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50"
    >
      {isPending ? "Deleting..." : "Delete"}
    </button>
  );
}
