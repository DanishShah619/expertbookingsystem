"use client";

import { logout } from "@/app/actions/auth";
import { useTransition } from "react";

export function LogoutButton() {
  const [isPending, startTransition] = useTransition();

  return (
    <form
      action={() => {
        startTransition(() => {
          logout();
        });
      }}
    >
      <button
        type="submit"
        disabled={isPending}
        className="rounded-md border border-rose-200 bg-white px-4 py-2 text-sm font-bold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50"
      >
        {isPending ? "Signing out…" : "Sign out"}
      </button>
    </form>
  );
}
