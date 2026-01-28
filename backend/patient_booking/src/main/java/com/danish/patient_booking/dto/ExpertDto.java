// ExpertDto.java
package com.danish.patient_booking.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpertDto {
    private Long       id;
    private String     name;
    private String     title;
    private String     bio;
    private String     photoUrl;
    private String     tags;
    private BigDecimal sessionPrice;
    private String     currency;
    private LocalDateTime createdAt;
}
