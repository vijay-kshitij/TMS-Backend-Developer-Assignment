package com.kshitij.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Load Entity - Represents a shipment request
 *
 * Optimistic Locking: @Version prevents concurrent modifications
 * Indexes: Added on frequently queried fields (status, shipperId)
 */
@Entity
@Table(name = "load", indexes = {
        @Index(name = "idx_load_status", columnList = "status"),
        @Index(name = "idx_load_shipper", columnList = "shipperId"),
        @Index(name = "idx_load_date_posted", columnList = "datePosted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Load {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID loadId;

    @Column(nullable = false, length = 50)
    private String shipperId;

    @Column(nullable = false, length = 100)
    private String loadingCity;

    @Column(nullable = false, length = 100)
    private String unloadingCity;

    @Column(nullable = false)
    private LocalDateTime loadingDate;

    @Column(nullable = false, length = 100)
    private String productType;

    @Column(nullable = false)
    private double weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WeightUnit weightUnit;

    @Column(nullable = false, length = 50)
    private String truckType;

    @Column(nullable = false)
    private int noOfTrucks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoadStatus status;

    @Column(nullable = false)
    private LocalDateTime datePosted;

    @Column(nullable = false)
    private int remainingTrucks;

    /**
     * Optimistic Locking: Prevents concurrent modifications
     * Automatically incremented by JPA on each update
     */
    @Version
    private Long version;
}