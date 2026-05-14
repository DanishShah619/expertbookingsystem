import { cookies } from "next/headers";

export const AUTH_COOKIE_NAME = "auth_token";

export async function getServerAuthToken(): Promise<string> {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE_NAME)?.value;
  return token || "";
}
