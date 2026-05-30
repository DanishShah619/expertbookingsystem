ALTER TABLE payments
    ADD COLUMN user_id BIGINT,
    ADD COLUMN slot_id BIGINT,
    ADD COLUMN expires_at TIMESTAMP NULL;

UPDATE payments p
SET user_id = b.user_id,
    slot_id = b.slot_id
FROM bookings b
WHERE p.booking_id = b.id
  AND p.user_id IS NULL
  AND p.slot_id IS NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_payments_slot FOREIGN KEY (slot_id) REFERENCES time_slots(id);

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS chk_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED', 'EXPIRED', 'CANCELLED', 'REFUND_FAILED'));

CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_slot ON payments(slot_id);

CREATE UNIQUE INDEX idx_bookings_payment_intent_unique ON bookings(payment_intent_id);
