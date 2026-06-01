export type Role = "ADMIN" | "EXPERT" | "USER";

export type SlotStatus = "AVAILABLE" | "LOCKED" | "BOOKED";

export type BookingStatus = "CONFIRMED" | "CANCELLED";

export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "REFUNDED";

export type IsoDateTime = string;

export type CurrencyCode = "INR" | string;

export interface ApiErrorBody {
  timestamp?: IsoDateTime;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  [key: string]: unknown;
}

export interface UserDto {
  id: number;
  googleId: string | null;
  email: string;
  name: string | null;
  pictureUrl: string | null;
  role: Role;
  expertId: number | null;
}

export interface UserProfileDto {
  id: number;
  email: string;
  name: string | null;
  pictureUrl: string | null;
  role: Role;
  totalBookings: number;
  upcomingBookings: number;
  completedBookings: number;
  cancelledBookings: number;
}

export interface SpecialtyDto {
  id: number;
  name: string;
  slug: string;
}

export interface ExpertDto {
  id: number;
  name: string;
  title: string;
  bio: string | null;
  photoUrl: string | null;
  specialty: SpecialtyDto | null;
  tags: string | null;
  sessionPrice: number;
  currency: CurrencyCode;
  createdAt: IsoDateTime;
}

export interface ExpertProfileDto extends ExpertDto {
  totalBookings: number;
  upcomingBookings: number;
  completedBookings: number;
}

export interface TimeSlotDto {
  id: number;
  expertId: number;
  startTime: IsoDateTime;
  endTime: IsoDateTime;
  status: SlotStatus;
  lockExpiresAt: IsoDateTime | null;
}

export interface SlotUpdateEvent {
  slotId: number;
  expertId: number;
  status: SlotStatus;
  startTime: IsoDateTime;
  endTime: IsoDateTime;
  lockExpiresAt: IsoDateTime | null;
}

export interface BookingDto {
  id: number;
  slotId: number;
  expertId: number;
  expertName: string;
  startTime: IsoDateTime;
  endTime: IsoDateTime;
  amountPaid: number;
  currency: CurrencyCode;
  status: BookingStatus | string;
  bookedAt: IsoDateTime;
  paymentIntentId?: string;
}

export interface ExpertBookingDto {
  bookingId: number;
  slotId: number;
  startTime: IsoDateTime;
  endTime: IsoDateTime;
  patientName: string | null;
  patientEmail: string;
  amountPaid: number;
  currency: CurrencyCode;
  status: BookingStatus | string;
  bookedAt: IsoDateTime;
  upcoming?: boolean;
  today?: boolean;
  isUpcoming?: boolean;
  isToday?: boolean;
}

export interface ExpertCreateRequest {
  userId: number;
  specialtyId: number;
  name: string;
  title: string;
  bio?: string | null;
  photoUrl?: string | null;
  tags?: string | null;
  sessionPrice: number;
  currency: CurrencyCode;
}

export interface SlotCreateRequest {
  expertId: number;
  startTime: IsoDateTime;
  endTime: IsoDateTime;
}

export interface SlotLockResponse {
  lockToken: string;
  expiresAt: IsoDateTime;
  clientSecret: string;
  amountInCents: number;
  currency: CurrencyCode;
}
