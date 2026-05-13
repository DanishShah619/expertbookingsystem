CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    slot_id BIGINT UNIQUE NOT NULL,
    payment_intent_id VARCHAR(255) NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    booked_at TIMESTAMP NULL DEFAULT NULL,
    status ENUM('CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_slot FOREIGN KEY (slot_id) REFERENCES time_slots(id)
);

CREATE INDEX idx_bookings_payment_intent ON bookings(payment_intent_id);
CREATE INDEX idx_bookings_user_booked_at ON bookings(user_id, booked_at);
