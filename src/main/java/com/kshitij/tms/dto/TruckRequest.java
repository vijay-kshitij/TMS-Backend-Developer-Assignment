package com.kshitij.tms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for truck availability information
 * Used as nested object in TransporterRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TruckRequest {

    @NotBlank(message = "Truck type is required")
    @Size(min = 2, max = 50, message = "Truck type must be between 2 and 50 characters")
    private String truckType;

    @Min(value = 0, message = "Truck count cannot be negative")
    @Max(value = 1000, message = "Truck count cannot exceed 1000")
    private int count;
}