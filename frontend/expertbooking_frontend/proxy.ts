import { NextRequest, NextResponse } from "next/server";
import type { Role, UserDto } from "@/types/api";

const AUTH_COOKIE_NAME = "auth_token";
const API_BASE_URL = getApiBaseUrl();

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

  if (matchesPathPrefix(pathname, adminPrefix) && user.role !== "ADMIN") {
    return NextResponse.redirect(new URL(getRoleHome(user.role), request.url));
  }

  if (matchesPathPrefix(pathname, expertPrefix) && user.role !== "EXPERT") {
    return NextResponse.redirect(new URL(getRoleHome(user.role), request.url));
  }

  return NextResponse.next();
}

function isProtectedPath(pathname: string) {
  return (
    authenticatedPrefixes.some((prefix) => matchesPathPrefix(pathname, prefix)) ||
    matchesPathPrefix(pathname, adminPrefix) ||
    matchesPathPrefix(pathname, expertPrefix) ||
    /^\/experts\/[^/]+\/checkout$/.test(pathname)
  );
}

function matchesPathPrefix(pathname: string, prefix: string) {
  return pathname === prefix || pathname.startsWith(`${prefix}/`);
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

function getApiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_URL;

  if (value) {
    return value.replace(/\/$/, "");
  }

  if (process.env.NODE_ENV !== "production") {
    return "http://localhost:8080";
  }

  throw new Error("NEXT_PUBLIC_API_URL must be set in production.");
}
