package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Expert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertRepository extends JpaRepository<Expert, Long> {
    // findAll() and findById() from JpaRepository are enough for now
}