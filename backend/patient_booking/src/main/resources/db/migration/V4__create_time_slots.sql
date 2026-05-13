CREATE TABLE time_slots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id BIGINT NOT NULL,
    status ENUM('AVAILABLE', 'LOCKED', 'BOOKED') NOT NULL DEFAULT 'AVAILABLE',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_time_slots_expert FOREIGN KEY (expert_id) REFERENCES experts(id)
);

CREATE INDEX idx_time_slots_expert_start ON time_slots(expert_id, start_time);
