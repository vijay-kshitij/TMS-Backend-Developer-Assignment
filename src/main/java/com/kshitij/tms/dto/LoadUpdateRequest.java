package com.kshitij.tms.dto;

import com.kshitij.tms.entity.WeightUnit;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for updating an existing load
 * All fields are optional (nullable) but validated if provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadUpdateRequest {

    @Size(min = 2, max = 100, message = "Loading city must be between 2 and 100 characters")
    private String loadingCity;

    @Size(min = 2, max = 100, message = "Unloading city must be between 2 and 100 characters")
    private String unloadingCity;

    @Future(message = "Loading date must be in the future")
    private LocalDateTime loadingDate;

    @Size(min = 2, max = 100, message = "Product type must be between 2 and 100 characters")
    private String productType;

    @Positive(message = "Weight must be positive")
    @DecimalMax(value = "100000.0", message = "Weight cannot exceed 100000")
    private Double weight;

    private WeightUnit weightUnit;

    @Size(min = 2, max = 50, message = "Truck type must be between 2 and 50 characters")
    private String truckType;

    @Min(value = 1, message = "Number of trucks must be at least 1")
    @Max(value = 100, message = "Number of trucks cannot exceed 100")
    private Integer noOfTrucks;
}