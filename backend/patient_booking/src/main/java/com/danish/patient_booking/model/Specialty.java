package com.danish.patient_booking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "Orthopedics", "Cardiology", "ENT", "Dermatology"
    @Column(unique = true, nullable = false)
    private String name;

    // URL-friendly slug for frontend filtering
    // e.g. "orthopedics", "cardiology", "ent"
    @Column(unique = true, nullable = false)
    private String slug;
}
