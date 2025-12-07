package com.kshitij.tms.dto;

import com.kshitij.tms.entity.WeightUnit;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for creating a new load
 * All fields are validated to ensure data integrity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadRequest {

    @NotBlank(message = "Shipper ID is required")
    @Size(min = 3, max = 50, message = "Shipper ID must be between 3 and 50 characters")
    private String shipperId;

    @NotBlank(message = "Loading city is required")
    @Size(min = 2, max = 100, message = "Loading city must be between 2 and 100 characters")
    private String loadingCity;

    @NotBlank(message = "Unloading city is required")
    @Size(min = 2, max = 100, message = "Unloading city must be between 2 and 100 characters")
    private String unloadingCity;

    @NotNull(message = "Loading date is required")
    @Future(message = "Loading date must be in the future")
    private LocalDateTime loadingDate;

    @NotBlank(message = "Product type is required")
    @Size(min = 2, max = 100, message = "Product type must be between 2 and 100 characters")
    private String productType;

    @Positive(message = "Weight must be positive")
    @DecimalMax(value = "100000.0", message = "Weight cannot exceed 100000")
    private double weight;

    @NotNull(message = "Weight unit is required (KG or TON)")
    private WeightUnit weightUnit;

    @NotBlank(message = "Truck type is required")
    @Size(min = 2, max = 50, message = "Truck type must be between 2 and 50 characters")
    private String truckType;

    @Min(value = 1, message = "Number of trucks must be at least 1")
    @Max(value = 100, message = "Number of trucks cannot exceed 100")
    private int noOfTrucks;
}