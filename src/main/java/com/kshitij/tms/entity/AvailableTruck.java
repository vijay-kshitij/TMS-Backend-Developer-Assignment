package com.kshitij.tms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * AvailableTruck - Embeddable entity for truck availability
 *
 * This is NOT a separate table, but part of Transporter entity
 * Stored in 'transporter_trucks' table
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTruck {

    @Column(nullable = false, length = 50)
    private String truckType;

    @Column(nullable = false)
    private int count;
}