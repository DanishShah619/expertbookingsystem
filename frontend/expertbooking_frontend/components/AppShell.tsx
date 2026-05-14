import Link from "next/link";
import { appRoutes } from "@/lib/routes";
import { NavUser } from "@/components/NavUser";
import { getCurrentAuthUser } from "@/lib/api/auth";
import { getServerAuthToken } from "@/lib/auth";
import type { Role, UserDto } from "@/types/api";

const navLinks: Record<Role | "GUEST", { href: string; label: string }[]> = {
  GUEST: [{ href: appRoutes.experts, label: "Browse experts" }],
  USER: [
    { href: appRoutes.experts, label: "Experts" },
    { href: appRoutes.myBookings, label: "Bookings" },
    { href: appRoutes.dashboard, label: "Dashboard" },
  ],
  EXPERT: [
    { href: appRoutes.expertDashboard, label: "Dashboard" },
    { href: appRoutes.expertBookings, label: "Bookings" },
    { href: appRoutes.expertSchedule, label: "Schedule" },
  ],
  ADMIN: [
    { href: appRoutes.adminExperts, label: "Experts" },
    { href: appRoutes.adminSlots, label: "Slots" },
    { href: appRoutes.adminSpecialties, label: "Specialties" },
  ],
};

export async function AppShell({ children }: { children: React.ReactNode }) {
  const token = await getServerAuthToken();
  const user = token ? await getCurrentAuthUser(token, { next: { revalidate: 0 } }).catch(() => null) : null;
  const links = navLinks[user?.role ?? "GUEST"];
  const navLabel = getNavLabel(user);

  return (
    <div className="min-h-screen bg-[#f7fbff] text-slate-950">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <Link href={appRoutes.home} className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-lg bg-slate-950 text-sm font-black text-white shadow-sm">
              EB
            </span>
            <span>
              <span className="block text-base font-bold tracking-tight">Expert Booking</span>
              <span className="block text-xs font-medium text-slate-500">{navLabel}</span>
            </span>
          </Link>

          <nav className="flex flex-wrap items-center gap-2 text-sm font-semibold text-slate-600">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-md px-3 py-2 transition hover:bg-cyan-50 hover:text-cyan-700"
              >
                {link.label}
              </Link>
            ))}
            <span className="mx-1 hidden h-5 w-px bg-slate-200 sm:block" />
            <NavUser user={user} />
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{children}</main>
    </div>
  );
}

function getNavLabel(user: UserDto | null): string {
  if (!user) {
    return "Browse first, sign in to book";
  }

  if (user.role === "ADMIN") {
    return "Admin workspace";
  }

  if (user.role === "EXPERT") {
    return "Expert workspace";
  }

  return "Patient workspace";
}
