"use server";

import { revalidateTag } from "next/cache";
import { getServerAuthToken } from "@/lib/auth";
import { cancelBooking } from "@/lib/api/bookings";
import { cacheTags } from "@/lib/api/cache-keys";

export async function refreshUserBookingsAfterPaymentAction() {
  const token = await getServerAuthToken();

  if (!token) {
    return { ok: false, error: "Please sign in to view your booking." };
  }

  revalidateTag(cacheTags.userBookings(), "default");
  revalidateTag(cacheTags.userProfile(), "default");

  return { ok: true, error: null };
}

export async function cancelBookingAction(bookingId: number, expertId: number) {
  const token = await getServerAuthToken();
  try {
    await cancelBooking(token, bookingId);

    // Purge caches so the UI reflects the cancelled status
    revalidateTag(cacheTags.userBookings(), "default");
    revalidateTag(cacheTags.expertBookings("all"), "default");
    revalidateTag(cacheTags.expertBookings("upcoming"), "default");
    revalidateTag(cacheTags.expertBookings("today"), "default");
    revalidateTag(cacheTags.expertSlots(expertId), "default");

  } catch (error) {
    console.error("Failed to cancel booking", error);
  }
}
