export const cacheTags = {
  authUser: (userId?: number | string) => compactTag("auth-user", userId),
  userProfile: (userId?: number | string) => compactTag("user-profile", userId),
  userBookings: (userId?: number | string) => compactTag("user-bookings", userId),
  experts: "experts",
  expert: (expertId: number | string) => `expert:${expertId}`,
  expertSlots: (expertId: number | string) => `expert-slots:${expertId}`,
  specialties: "specialties",
  expertProfile: (expertId?: number | string) => compactTag("expert-profile", expertId),
  expertBookings: (expertId?: number | string) => compactTag("expert-bookings", expertId),
  adminExperts: "admin-experts",
  adminSpecialties: "admin-specialties",
  adminBookings: "admin-bookings",
} as const;

export const cacheKeys = {
  experts: (params?: { specialty?: string; search?: string }) => [
    "experts",
    params?.specialty ?? "",
    params?.search ?? "",
  ],
  expert: (expertId: number | string) => ["expert", String(expertId)],
  expertSlots: (expertId: number | string) => ["expert-slots", String(expertId)],
  specialties: () => ["specialties"],
  userBookings: (scope: "all" | "upcoming" | "past" = "all") => ["user-bookings", scope],
  expertBookings: (scope: "all" | "upcoming" | "today" = "all") => ["expert-bookings", scope],
} as const;

function compactTag(prefix: string, id?: number | string): string {
  return id === undefined || id === null ? prefix : `${prefix}:${id}`;
}
