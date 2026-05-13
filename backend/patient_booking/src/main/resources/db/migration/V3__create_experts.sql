CREATE TABLE experts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE,
    name VARCHAR(255),
    title TEXT NOT NULL,
    bio TEXT NOT NULL,
    photo_url TEXT,
    specialty_id BIGINT NOT NULL,
    tags TEXT,
    session_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_experts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_experts_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);
