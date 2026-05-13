// ExpertCreateRequest.java
package com.danish.patient_booking.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpertCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Title is required")
    private String title;

    private String bio;
    private String photoUrl;
    private String tags;
    @NotNull(message = "Specialty ID is required")
    private Long specialtyId;

    @NotNull(message = "Session price is required")
    @DecimalMin(value = "50.00", message = "Minimum session price is ₹50")
    private BigDecimal sessionPrice;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code e.g. INR")
    private String currency;


    @NotNull(message = "User ID is required to link expert to a login account")
    private Long userId;
}
