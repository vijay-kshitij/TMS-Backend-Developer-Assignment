package com.kshitij.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Booking Entity - Represents a confirmed booking (accepted bid)
 *
 * Foreign Keys: loadId, bidId, and transporterId reference respective tables
 * Optimistic Locking: @Version prevents concurrent modifications
 * Indexes: Added on loadId, transporterId, status for query performance
 */
@Entity
@Table(name = "booking",
        indexes = {
                @Index(name = "idx_booking_load", columnList = "loadId"),
                @Index(name = "idx_booking_transporter", columnList = "transporterId"),
                @Index(name = "idx_booking_bid", columnList = "bidId"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_date", columnList = "bookedAt")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID bookingId;

    /**
     * Foreign Key to Load table
     * Stored as UUID for simplicity
     */
    @Column(nullable = false, name = "loadId")
    private UUID loadId;

    /**
     * Foreign Key to Bid table
     * One booking corresponds to one accepted bid
     */
    @Column(nullable = false, unique = true, name = "bidId")
    private UUID bidId;

    /**
     * Foreign Key to Transporter table
     * Stored as UUID for simplicity
     */
    @Column(nullable = false, name = "transporterId")
    private UUID transporterId;

    @Column(nullable = false)
    private int allocatedTrucks;

    @Column(nullable = false)
    private double finalRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Column(nullable = false, length = 50)
    private String truckType;

    /**
     * Optimistic Locking: Prevents concurrent modifications
     * Automatically incremented by JPA on each update
     */
    @Version
    private Long version;
}