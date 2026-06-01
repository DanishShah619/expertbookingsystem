"use server";

import { revalidateTag } from "next/cache";
import { getServerAuthToken } from "@/lib/auth";
import { cancelBooking } from "@/lib/api/bookings";
import { getUserBookings } from "@/lib/api/user";
import { cacheTags } from "@/lib/api/cache-keys";


export async function refreshUserBookingsAfterPaymentAction(
  paymentIntentId?: string,
): Promise<{ ok: boolean; confirmed: boolean; error: string | null }> {
  const token = await getServerAuthToken();

  if (!token) {
    return { ok: false, confirmed: false, error: "Please sign in to view your booking." };
  }

  // Always bust the fetch cache so the next render fetches fresh data from the backend
  revalidateTag(cacheTags.userBookings(), { expire: 0 });
  revalidateTag(cacheTags.userProfile(), { expire: 0 });

  // If we have a paymentIntentId, verify the booking actually exists in the DB
  // before telling the client it is safe to stop polling.
  if (paymentIntentId) {
    try {
      const bookings = await getUserBookings(token, { cache: "no-store" });
      const confirmed = bookings.some((b) => b.paymentIntentId === paymentIntentId);
      return { ok: true, confirmed, error: null };
    } catch {
      // Backend temporarily unreachable — keep polling
      return { ok: true, confirmed: false, error: null };
    }
  }

  return { ok: true, confirmed: false, error: null };
}


export async function cancelBookingAction(bookingId: number, expertId: number) {
  const token = await getServerAuthToken();
  if (!token) {
    return { ok: false, error: "Please sign in before cancelling a booking." };
  }

  try {
    await cancelBooking(token, bookingId);

    // Purge caches so the UI reflects the cancelled status
    revalidateTag(cacheTags.userBookings(), { expire: 0 });
    revalidateTag(cacheTags.expertBookings("all"), { expire: 0 });
    revalidateTag(cacheTags.expertBookings("upcoming"), { expire: 0 });
    revalidateTag(cacheTags.expertBookings("today"), { expire: 0 });
    revalidateTag(cacheTags.expertSlots(expertId), { expire: 0 });

    return { ok: true, error: null };
  } catch (error) {
    console.error("Failed to cancel booking", error);
    return { ok: false, error: getErrorMessage(error) };
  }
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Booking could not be cancelled. Please try again or contact support.";
}
