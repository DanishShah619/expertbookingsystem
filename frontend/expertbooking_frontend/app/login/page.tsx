import { AppShell } from "@/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { GoogleLoginButton } from "@/components/GoogleLoginButton";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string }>;
}) {
  const { next } = await searchParams;

  return (
    <AppShell>
      <div className="mx-auto max-w-xl">
        <PageHeader
          eyebrow="Secure access"
          title="Sign in with Google"
          description="The backend expects a Google ID token as a Bearer token. This screen is ready for the Google OAuth button once the auth package is added."
        />
        <div className="rounded-lg border border-slate-100 bg-white p-6 shadow-sm">
          <div className="mb-4">
            <GoogleLoginButton nextPath={next} />
          </div>
          <p className="text-sm leading-6 text-slate-500 text-center mt-6">
            Admin, expert, and user access all use the same Google login. The visible role is resolved from
            the backend user record.
          </p>
        </div>
      </div>
    </AppShell>
  );
}
