import Link from "next/link";
import Image from "next/image";
import { appRoutes } from "@/lib/routes";
import { LogoutButton } from "@/components/LogoutButton";
import type { UserDto } from "@/types/api";

export function NavUser({ user }: { user: UserDto | null }) {
  if (!user) {
    return (
      <Link
        href={appRoutes.login}
        className="rounded-md bg-slate-950 px-4 py-2 text-sm font-bold text-white shadow-sm transition hover:bg-cyan-700"
      >
        Sign in
      </Link>
    );
  }

  const roleBadge: Record<string, string> = {
    ADMIN: "bg-rose-100 text-rose-700",
    EXPERT: "bg-violet-100 text-violet-700",
    USER: "bg-cyan-100 text-cyan-700",
  };

  return (
    <div className="flex items-center gap-3">
      <div className="flex items-center gap-2.5 rounded-full border border-slate-200 bg-white pl-1 pr-3 py-1 shadow-sm">
        {user.pictureUrl ? (
          <Image
            src={user.pictureUrl}
            alt={user.name ?? "User"}
            width={30}
            height={30}
            className="rounded-full object-cover"
          />
        ) : (
          <span className="grid size-[30px] place-items-center rounded-full bg-cyan-500 text-xs font-black text-white">
            {(user.name ?? user.email)[0].toUpperCase()}
          </span>
        )}
        <div className="leading-tight">
          <p className="text-sm font-bold text-slate-900">{user.name ?? user.email}</p>
          <span
            className={`inline-block rounded px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${roleBadge[user.role] ?? roleBadge.USER}`}
          >
            {user.role}
          </span>
        </div>
      </div>

      <LogoutButton />
    </div>
  );
}
