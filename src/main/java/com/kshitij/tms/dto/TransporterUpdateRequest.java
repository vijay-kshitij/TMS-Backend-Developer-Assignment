package com.kshitij.tms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTO for updating transporter's available trucks
 * Replaces the entire truck list with new values
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransporterUpdateRequest {

    @NotEmpty(message = "At least one truck type must be provided")
    @Valid  // This ensures validation of nested TruckRequest objects
    private List<TruckRequest> availableTrucks;
}