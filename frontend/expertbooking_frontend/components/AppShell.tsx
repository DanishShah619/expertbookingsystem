import Link from "next/link";
import { appRoutes } from "@/lib/routes";

const primaryLinks = [
  { href: appRoutes.experts, label: "Experts" },
  { href: appRoutes.myBookings, label: "Bookings" },
  { href: appRoutes.dashboard, label: "Dashboard" },
];

const roleLinks = [
  { href: appRoutes.adminExperts, label: "Admin" },
  { href: appRoutes.expertDashboard, label: "Expert" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-[#f7fbff] text-slate-950">
      <header className="sticky top-0 z-20 border-b border-sky-100 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <Link href={appRoutes.home} className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-lg bg-cyan-500 text-sm font-black text-white shadow-sm">
              EB
            </span>
            <span>
              <span className="block text-base font-bold tracking-tight">Expert Booking</span>
              <span className="block text-xs font-medium text-slate-500">Real-time session booking</span>
            </span>
          </Link>

          <nav className="flex flex-wrap items-center gap-2 text-sm font-semibold text-slate-600">
            {primaryLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-md px-3 py-2 transition hover:bg-cyan-50 hover:text-cyan-700"
              >
                {link.label}
              </Link>
            ))}
            <span className="mx-1 hidden h-5 w-px bg-slate-200 sm:block" />
            {roleLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-md px-3 py-2 transition hover:bg-violet-50 hover:text-violet-700"
              >
                {link.label}
              </Link>
            ))}
            <Link
              href={appRoutes.login}
              className="rounded-md bg-slate-950 px-4 py-2 text-white shadow-sm transition hover:bg-cyan-700"
            >
              Sign in
            </Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{children}</main>
    </div>
  );
}
