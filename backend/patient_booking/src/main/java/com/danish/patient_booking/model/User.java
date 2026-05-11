package com.danish.patient_booking.model;



import com.danish.patient_booking.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = true, unique = true)
    private String googleId;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(name = "picture_url", columnDefinition = "TEXT")
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (role == null) {
            role = Role.USER;
        }
    }


     @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    @OneToOne
    @JoinColumn(name = "expert_id")
    private Expert expertProfile;




}
