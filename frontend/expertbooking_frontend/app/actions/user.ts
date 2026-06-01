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

  revalidateTag(cacheTags.userBookings(), { expire: 0 });
  revalidateTag(cacheTags.userProfile(), { expire: 0 });

  return { ok: true, error: null };
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
