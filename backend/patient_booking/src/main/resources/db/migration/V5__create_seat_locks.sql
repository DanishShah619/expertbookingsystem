CREATE TABLE seat_locks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_id BIGINT UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    lock_token VARCHAR(255) UNIQUE NOT NULL,
    payment_intent_id VARCHAR(255),
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_seat_locks_slot FOREIGN KEY (slot_id) REFERENCES time_slots(id),
    CONSTRAINT fk_seat_locks_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_seat_locks_expires_at ON seat_locks(expires_at);
CREATE INDEX idx_seat_locks_payment_intent ON seat_locks(payment_intent_id);
