"use server";

import { revalidateTag } from "next/cache";
import { cacheTags } from "@/lib/api/cache-keys";
import { releaseSlotLock, lockSlot } from "@/lib/api/slots";
import { getServerAuthToken } from "@/lib/auth";
import type { SlotLockResponse } from "@/types/api";

type CheckoutActionResult<T = void> =
  | { data: T; error: null }
  | { data: null; error: string };

export async function lockSlotForCheckoutAction(
  slotId: number,
  expertId: number,
): Promise<CheckoutActionResult<SlotLockResponse>> {
  const token = await getServerAuthToken();

  if (!token) {
    return { data: null, error: "Please sign in before booking this slot." };
  }

  try {
    const lock = await lockSlot(token, slotId, { cache: "no-store" });
    revalidateTag(cacheTags.expertSlots(expertId), { expire: 0 });
    return { data: lock, error: null };
  } catch (error) {
    console.error("Slot lock failed:", error);
    return { data: null, error: getErrorMessage(error) };
  }
}

export async function releaseSlotForCheckoutAction(
  slotId: number,
  expertId: number,
): Promise<CheckoutActionResult> {
  const token = await getServerAuthToken();

  if (!token) {
    return { data: null, error: "Please sign in before changing this booking." };
  }

  try {
    await releaseSlotLock(token, slotId, { cache: "no-store" });
    revalidateTag(cacheTags.expertSlots(expertId), { expire: 0 });
    return { data: undefined, error: null };
  } catch (error) {
    console.error("Slot release failed:", error);
    return { data: null, error: getErrorMessage(error) };
  }
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Payment setup failed. Please try another slot.";
}
