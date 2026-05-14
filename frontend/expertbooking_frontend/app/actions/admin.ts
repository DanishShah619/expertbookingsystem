"use server";

import { revalidateTag } from "next/cache";
import { getServerAuthToken } from "@/lib/auth";
import {
  createSpecialty,
  deleteSpecialty,
  createExpert,
  deleteExpert,
  createSlot,
  deleteSlot,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/http";
import { cacheTags } from "@/lib/api/cache-keys";

export type ActionState = {
  message: string | null;
  error: string | null;
};

// --- Specialties ---
export async function addSpecialtyAction(prevState: ActionState, formData: FormData): Promise<ActionState> {
  const token = await getServerAuthToken();
  const name = formData.get("name") as string;

  if (!name || name.trim() === "") {
    return { message: null, error: "Specialty name is required." };
  }

  try {
    await createSpecialty(token, name);
    revalidateTag(cacheTags.adminSpecialties, "default");
    revalidateTag(cacheTags.specialties, "default");
    return { message: "Specialty added successfully.", error: null };
  } catch (error) {
    return { message: null, error: getErrorMessage(error, "Failed to add specialty.") };
  }
}

export async function deleteSpecialtyAction(id: number) {
  const token = await getServerAuthToken();
  try {
    await deleteSpecialty(token, id);
    revalidateTag(cacheTags.adminSpecialties, "default");
    revalidateTag(cacheTags.specialties, "default");
  } catch (error) {
    console.error("Failed to delete specialty", error);
  }
}

// --- Experts ---
export async function addExpertAction(prevState: ActionState, formData: FormData): Promise<ActionState> {
  const token = await getServerAuthToken();
  const userId = formData.get("userId");
  const name = formData.get("name") as string;
  const specialtyId = formData.get("specialtyId");
  const sessionPrice = formData.get("sessionPrice");

  if (!userId || !name || !specialtyId || !sessionPrice) {
    return { message: null, error: "All fields are required." };
  }

  try {
    await createExpert(token, {
      userId: Number(userId),
      specialtyId: Number(specialtyId),
      name,
      title: "Consultant",
      sessionPrice: Number(sessionPrice),
      currency: "INR",
    });
    revalidateTag(cacheTags.adminExperts, "default");
    revalidateTag(cacheTags.experts, "default");
    return { message: "Expert added successfully.", error: null };
  } catch (error) {
    const message = getErrorMessage(error, "Something went wrong - please try again");
    console.error("Expert creation failed:", toLoggableError(error));
    return {
      message: null,
      error: message,
    };
  }
}

export async function deleteExpertAction(id: number) {
  const token = await getServerAuthToken();
  try {
    await deleteExpert(token, id);
    revalidateTag(cacheTags.adminExperts, "default");
    revalidateTag(cacheTags.experts, "default");
    revalidateTag(cacheTags.expert(id), "default");
  } catch (error) {
    console.error("Failed to delete expert", error);
  }
}

// --- Slots ---
export async function addSlotAction(prevState: ActionState, formData: FormData): Promise<ActionState> {
  const token = await getServerAuthToken();
  const expertId = formData.get("expertId");
  const startTime = formData.get("startTime") as string;
  const endTime = formData.get("endTime") as string;

  if (!expertId || !startTime || !endTime) {
    return { message: null, error: "All fields are required." };
  }

  try {
    await createSlot(token, {
      expertId: Number(expertId),
      startTime,
      endTime,
    });
    revalidateTag(cacheTags.expertSlots(Number(expertId)), "default");
    return { message: "Slot added successfully.", error: null };
  } catch (error) {
    return { message: null, error: getErrorMessage(error, "Failed to add slot.") };
  }
}

export async function deleteSlotAction(id: number, expertId: number) {
  const token = await getServerAuthToken();
  try {
    await deleteSlot(token, id);
    revalidateTag(cacheTags.expertSlots(expertId), "default");
  } catch (error) {
    console.error("Failed to delete slot", error);
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function toLoggableError(error: unknown) {
  if (error instanceof ApiError) {
    return {
      name: error.name,
      message: error.message,
      status: error.status,
      url: error.url,
      body: error.body,
    };
  }

  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message,
      stack: error.stack,
    };
  }

  return error;
}
