package com.kshitij.tms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Transporter Entity - Represents a transport company
 *
 * Optimistic Locking: @Version prevents concurrent truck count modifications
 * Embedded Collection: availableTrucks stored in separate table
 * Constraints: Company name must be unique
 */
@Entity
@Table(name = "transporter",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transporter_company", columnNames = "companyName")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID transporterId;

    @Column(nullable = false, unique = true, length = 100)
    private String companyName;

    @Column(nullable = false)
    private double rating;  // value between 1.0 and 5.0

    /**
     * Available trucks collection stored in separate table
     * Cascading: All operations on transporter cascade to trucks
     * Orphan Removal: Trucks deleted when removed from list
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "transporter_trucks",
            joinColumns = @JoinColumn(name = "transporter_id"),
            foreignKey = @ForeignKey(name = "fk_trucks_transporter")
    )
    private List<AvailableTruck> availableTrucks;

    /**
     * Optimistic Locking: Critical for truck count updates
     * Prevents race conditions when multiple bookings happen simultaneously
     */
    @Version
    private Long version;
}