"use server";

import { cookies } from "next/headers";
import { isRedirectError } from "next/dist/client/components/redirect-error";
import { redirect } from "next/navigation";
import { AUTH_COOKIE_NAME } from "@/lib/auth";
import { getCurrentAuthUser } from "@/lib/api/auth";
import { appRoutes } from "@/lib/routes";

export async function handleGoogleLogin(idToken: string) {
  // Store the token in an HTTP-only cookie
  const cookieStore = await cookies();
  cookieStore.set(AUTH_COOKIE_NAME, idToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 7, // 1 week
  });

  try {
    // Fetch user profile from the backend using the token
    const user = await getCurrentAuthUser(idToken, { next: { revalidate: 0 } });

    // Redirect based on role — redirect() throws NEXT_REDIRECT internally
    if (user.role === "ADMIN") {
      redirect(appRoutes.adminExperts);
    } else if (user.role === "EXPERT") {
      redirect(appRoutes.expertDashboard);
    } else {
      redirect(appRoutes.dashboard);
    }
  } catch (error) {
    // redirect() throws a special NEXT_REDIRECT error — re-throw it so Next.js
    // can handle the redirect correctly. Never treat it as a real failure.
    if (isRedirectError(error)) {
      throw error;
    }
    // Only reach here for genuine API/network errors
    console.error("Login failed — API error:", error);
    cookieStore.delete(AUTH_COOKIE_NAME);
    redirect(appRoutes.login);
  }
}

export async function logout() {
  const cookieStore = await cookies();
  cookieStore.delete(AUTH_COOKIE_NAME);
  redirect(appRoutes.login);
}
