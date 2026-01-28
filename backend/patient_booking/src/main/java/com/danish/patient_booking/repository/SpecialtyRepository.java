package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findBySlug(String slug);

    boolean existsByName(String name);
}