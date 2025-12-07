package com.kshitij.tms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a booking (accepting a bid)
 * Validates truck allocation and rate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @Min(value = 1, message = "Allocated trucks must be at least 1")
    @Max(value = 100, message = "Allocated trucks cannot exceed 100")
    private int allocatedTrucks;

    @Positive(message = "Final rate must be positive")
    @DecimalMin(value = "0.01", message = "Final rate must be at least 0.01")
    @DecimalMax(value = "1000000.0", message = "Final rate cannot exceed 1000000")
    private double finalRate;
}