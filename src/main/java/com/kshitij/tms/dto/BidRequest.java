package com.kshitij.tms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
 * DTO for submitting a bid
 * Validates business rules at API layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "Load ID is required")
    private UUID loadId;

    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;

    @Positive(message = "Proposed rate must be positive")
    @DecimalMin(value = "0.01", message = "Proposed rate must be at least 0.01")
    @DecimalMax(value = "1000000.0", message = "Proposed rate cannot exceed 1000000")
    private double proposedRate;

    @Min(value = 1, message = "Trucks offered must be at least 1")
    @Max(value = 100, message = "Trucks offered cannot exceed 100")
    private int trucksOffered;
}