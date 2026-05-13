package com.danish.patient_booking.repository;

import com.danish.patient_booking.model.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpertRepository extends JpaRepository<Expert, Long> {
    // findAll() and findById() from JpaRepository are enough for now
    List<Expert> findBySpecialtySlug(String slug);

    // Search by name OR specialty name — used by search bar
    @Query("""
        SELECT e FROM Expert e
        WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.specialty.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.tags) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<Expert> searchExperts(@Param("query") String query);

    // Filter by specialty ID
    List<Expert> findBySpecialtyId(Long specialtyId);

    Optional<Expert> findByUserId(Long userId);

}