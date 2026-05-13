package com.danish.patient_booking.model;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;


    @Column(name = "tags", columnDefinition = "TEXT", length = 500)
    private String tags;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimeSlot> timeSlots;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;


}
