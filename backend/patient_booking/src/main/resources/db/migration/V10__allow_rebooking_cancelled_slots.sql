-- Drop the unique constraint on slot_id (PostgreSQL creates bookings_slot_id_key for slot_id UNIQUE)
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_slot_id_key;

-- Create a conditional unique index that only allows one active (CONFIRMED) booking per slot
CREATE UNIQUE INDEX idx_bookings_active_slot ON bookings(slot_id) WHERE status = 'CONFIRMED';
