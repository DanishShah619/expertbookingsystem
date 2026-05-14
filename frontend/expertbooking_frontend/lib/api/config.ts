export const API_BASE_URL = getRequiredUrl(
  process.env.NEXT_PUBLIC_API_URL,
  "NEXT_PUBLIC_API_URL",
  "http://localhost:8080",
);

export const WS_BASE_URL =
  process.env.NEXT_PUBLIC_WS_URL?.replace(/\/$/, "") ?? "http://localhost:8080/ws";

function getRequiredUrl(value: string | undefined, name: string, developmentFallback: string) {
  if (value) {
    return value.replace(/\/$/, "");
  }

  if (process.env.NODE_ENV !== "production") {
    return developmentFallback;
  }

  throw new Error(`${name} must be set in production.`);
}
