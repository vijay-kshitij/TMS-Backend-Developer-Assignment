package com.kshitij.tms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTO for registering a new transporter
 * Validates company details and truck availability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransporterRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 3, max = 100, message = "Company name must be between 3 and 100 characters")
    private String companyName;

    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    private double rating;

    @NotEmpty(message = "At least one truck type must be provided")
    @Valid  // This ensures validation of nested TruckRequest objects
    private List<TruckRequest> availableTrucks;
}