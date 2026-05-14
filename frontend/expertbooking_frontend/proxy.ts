import { NextRequest, NextResponse } from "next/server";
import type { Role, UserDto } from "@/types/api";

const AUTH_COOKIE_NAME = "auth_token";
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

const authenticatedPrefixes = ["/dashboard", "/my-bookings"];
const adminPrefix = "/admin";
const expertPrefix = "/expert";

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get(AUTH_COOKIE_NAME)?.value;

  if (!isProtectedPath(pathname)) {
    return NextResponse.next();
  }

  if (!token) {
    return redirectToLogin(request);
  }

  const user = await getCurrentUser(token);

  if (!user) {
    const response = redirectToLogin(request);
    response.cookies.delete(AUTH_COOKIE_NAME);
    return response;
  }

  if (pathname.startsWith(adminPrefix) && user.role !== "ADMIN") {
    return NextResponse.redirect(new URL(getRoleHome(user.role), request.url));
  }

  if (pathname.startsWith(expertPrefix) && user.role !== "EXPERT") {
    return NextResponse.redirect(new URL(getRoleHome(user.role), request.url));
  }

  return NextResponse.next();
}

function isProtectedPath(pathname: string) {
  return (
    authenticatedPrefixes.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)) ||
    pathname.startsWith(adminPrefix) ||
    pathname.startsWith(expertPrefix) ||
    /^\/experts\/[^/]+\/checkout$/.test(pathname)
  );
}

function redirectToLogin(request: NextRequest) {
  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("next", `${request.nextUrl.pathname}${request.nextUrl.search}`);
  return NextResponse.redirect(loginUrl);
}

function getRoleHome(role: Role) {
  if (role === "ADMIN") {
    return "/admin/experts";
  }

  if (role === "EXPERT") {
    return "/expert/dashboard";
  }

  return "/dashboard";
}

async function getCurrentUser(token: string): Promise<UserDto | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as UserDto;
  } catch {
    return null;
  }
}

export const config = {
  matcher: ["/dashboard/:path*", "/my-bookings/:path*", "/admin/:path*", "/expert/:path*", "/experts/:id/checkout"],
};
