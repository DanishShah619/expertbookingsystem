package com.danish.patient_booking.model;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "experts")
@Builder
@Getter
@Setter
public class Expert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Column(name="title",nullable = false,columnDefinition = "TEXT")
    private  String title;

    @Column(name="bio",nullable = false,columnDefinition = "TEXT")
    private  String bio;

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(name = "session_price",nullable = false)
    private BigDecimal sessionPrice;


    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Builder.Default
    @Column(length = 3, nullable = false)
    private String currency = "INR";


    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }


}
