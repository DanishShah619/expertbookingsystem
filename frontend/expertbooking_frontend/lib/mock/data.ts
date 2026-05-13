import type { BookingDto, ExpertBookingDto, ExpertDto, SpecialtyDto, TimeSlotDto } from "@/types/api";

export const specialties: SpecialtyDto[] = [
  { id: 1, name: "Cardiology", slug: "cardiology" },
  { id: 2, name: "Orthopedics", slug: "orthopedics" },
  { id: 3, name: "ENT", slug: "ent" },
  { id: 4, name: "Dermatology", slug: "dermatology" },
  { id: 5, name: "Neurology", slug: "neurology" },
];

export const experts: ExpertDto[] = [
  {
    id: 1,
    name: "Dr. Anaya Rao",
    title: "Senior Cardiologist",
    bio: "Heart rhythm, hypertension, and preventive cardiac care.",
    photoUrl: null,
    specialty: specialties[0],
    tags: "Preventive,Interventional",
    sessionPrice: 800,
    currency: "INR",
    createdAt: "2026-05-01T09:00:00",
  },
  {
    id: 2,
    name: "Dr. Vikram Mehta",
    title: "Orthopedic Consultant",
    bio: "Joint pain, sports injuries, and post-operative rehabilitation.",
    photoUrl: null,
    specialty: specialties[1],
    tags: "Sports injury,Knee",
    sessionPrice: 650,
    currency: "INR",
    createdAt: "2026-05-02T09:00:00",
  },
  {
    id: 3,
    name: "Dr. Sarah Khan",
    title: "ENT Specialist",
    bio: "Sinus, hearing care, voice, and allergy consultation.",
    photoUrl: null,
    specialty: specialties[2],
    tags: "Sinus,Allergy",
    sessionPrice: 500,
    currency: "INR",
    createdAt: "2026-05-03T09:00:00",
  },
];

export const slots: TimeSlotDto[] = [
  {
    id: 101,
    expertId: 1,
    startTime: "2026-05-15T10:00:00",
    endTime: "2026-05-15T10:30:00",
    status: "AVAILABLE",
    lockExpiresAt: null,
  },
  {
    id: 102,
    expertId: 1,
    startTime: "2026-05-15T11:00:00",
    endTime: "2026-05-15T11:30:00",
    status: "LOCKED",
    lockExpiresAt: "2026-05-15T10:58:00",
  },
  {
    id: 103,
    expertId: 1,
    startTime: "2026-05-15T12:00:00",
    endTime: "2026-05-15T12:30:00",
    status: "BOOKED",
    lockExpiresAt: null,
  },
  {
    id: 201,
    expertId: 2,
    startTime: "2026-05-15T14:00:00",
    endTime: "2026-05-15T14:30:00",
    status: "AVAILABLE",
    lockExpiresAt: null,
  },
  {
    id: 301,
    expertId: 3,
    startTime: "2026-05-16T09:30:00",
    endTime: "2026-05-16T10:00:00",
    status: "AVAILABLE",
    lockExpiresAt: null,
  },
];

export const userBookings: BookingDto[] = [
  {
    id: 9001,
    slotId: 103,
    expertId: 1,
    expertName: "Dr. Anaya Rao",
    startTime: "2026-05-15T12:00:00",
    endTime: "2026-05-15T12:30:00",
    amountPaid: 800,
    currency: "INR",
    status: "CONFIRMED",
    bookedAt: "2026-05-14T09:15:00",
  },
  {
    id: 9002,
    slotId: 301,
    expertId: 3,
    expertName: "Dr. Sarah Khan",
    startTime: "2026-05-10T09:30:00",
    endTime: "2026-05-10T10:00:00",
    amountPaid: 500,
    currency: "INR",
    status: "CONFIRMED",
    bookedAt: "2026-05-08T13:25:00",
  },
];

export const expertBookings: ExpertBookingDto[] = [
  {
    bookingId: 9001,
    slotId: 103,
    startTime: "2026-05-15T12:00:00",
    endTime: "2026-05-15T12:30:00",
    patientName: "Aarav Sharma",
    patientEmail: "aarav@example.com",
    amountPaid: 800,
    currency: "INR",
    status: "CONFIRMED",
    bookedAt: "2026-05-14T09:15:00",
    upcoming: true,
    today: false,
  },
  {
    bookingId: 9003,
    slotId: 104,
    startTime: "2026-05-14T16:30:00",
    endTime: "2026-05-14T17:00:00",
    patientName: "Meera Iyer",
    patientEmail: "meera@example.com",
    amountPaid: 800,
    currency: "INR",
    status: "CONFIRMED",
    bookedAt: "2026-05-13T18:40:00",
    upcoming: true,
    today: true,
  },
];

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatTime(value: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatCurrency(amount: number, currency = "INR"): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}
