package com.danish.patient_booking.exception;

public class LockExpiredException extends RuntimeException {
    public LockExpiredException(String message) {
        super(message);
    }
}
