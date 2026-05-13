import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";

export default function LoginPage() {
  return (
    <AppShell>
      <div className="mx-auto max-w-xl">
        <PageHeader
          eyebrow="Secure access"
          title="Sign in with Google"
          description="The backend expects a Google ID token as a Bearer token. This screen is ready for the Google OAuth button once the auth package is added."
        />
        <div className="rounded-lg border border-slate-100 bg-white p-6 shadow-sm">
          <button
            type="button"
            className="mb-4 flex w-full items-center justify-center rounded-md bg-slate-950 px-5 py-3 text-sm font-bold text-white transition hover:bg-cyan-700"
          >
            Continue with Google
          </button>
          <p className="text-sm leading-6 text-slate-500">
            Admin, expert, and user access all use the same Google login. The visible role is resolved from
            the backend user record.
          </p>
        </div>
      </div>
    </AppShell>
  );
}
