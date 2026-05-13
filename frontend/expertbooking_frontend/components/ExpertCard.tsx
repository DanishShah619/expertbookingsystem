import Link from "next/link";
import { StatusPill } from "@/components/StatusPill";
import { appRoutes } from "@/lib/routes";
import { formatCurrency } from "@/lib/mock/data";
import type { ExpertDto } from "@/types/api";

export function ExpertCard({ expert }: { expert: ExpertDto }) {
  const initials = expert.name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2);

  return (
    <article className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
      <div className="mb-5 flex items-start gap-4">
        <div className="grid size-14 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-cyan-400 to-violet-500 font-black text-white">
          {initials}
        </div>
        <div className="min-w-0">
          <h2 className="truncate text-lg font-black text-slate-950">{expert.name}</h2>
          <p className="text-sm font-semibold text-slate-500">{expert.title}</p>
        </div>
      </div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <StatusPill status={expert.specialty?.name ?? "General"} />
        <span className="rounded-md bg-fuchsia-50 px-2.5 py-1 text-xs font-bold text-fuchsia-700 ring-1 ring-fuchsia-100">
          {formatCurrency(expert.sessionPrice, expert.currency)}
        </span>
      </div>
      <p className="mb-5 line-clamp-2 min-h-12 text-sm leading-6 text-slate-600">{expert.bio}</p>
      <Link
        href={appRoutes.expertDetail(expert.id)}
        className="inline-flex w-full items-center justify-center rounded-md bg-cyan-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-cyan-700"
      >
        View slots
      </Link>
    </article>
  );
}
