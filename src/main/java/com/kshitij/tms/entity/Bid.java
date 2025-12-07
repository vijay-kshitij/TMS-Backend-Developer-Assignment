package com.kshitij.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bid Entity - Represents a transporter's bid on a load
 *
 * Foreign Keys: loadId and transporterId reference Load and Transporter tables
 * Unique Constraint: Only one ACCEPTED bid allowed per load (via partial index in DB)
 * Indexes: Added on loadId, transporterId, status for query performance
 */
@Entity
@Table(name = "bid",
        indexes = {
                @Index(name = "idx_bid_load", columnList = "loadId"),
                @Index(name = "idx_bid_transporter", columnList = "transporterId"),
                @Index(name = "idx_bid_status", columnList = "status"),
                @Index(name = "idx_bid_submitted", columnList = "submittedAt")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID bidId;

    /**
     * Foreign Key to Load table
     * Stored as UUID for simplicity, with database-level FK constraint
     */
    @Column(nullable = false, name = "loadId")
    private UUID loadId;

    /**
     * Foreign Key to Transporter table
     * Stored as UUID for simplicity, with database-level FK constraint
     */
    @Column(nullable = false, name = "transporterId")
    private UUID transporterId;

    @Column(nullable = false)
    private double proposedRate;

    @Column(nullable = false)
    private int trucksOffered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    /**
     * Note: The unique constraint "only one ACCEPTED bid per load"
     * is best implemented as a partial unique index in PostgreSQL:
     *
     * CREATE UNIQUE INDEX uk_one_accepted_bid_per_load
     * ON bid (loadId)
     * WHERE status = 'ACCEPTED';
     *
     * This can be added via SQL migration script or manually in PostgreSQL.
     *
     * To add manually, connect to your PostgreSQL and run:
     * CREATE UNIQUE INDEX IF NOT EXISTS uk_one_accepted_bid_per_load
     * ON bid (load_id) WHERE status = 'ACCEPTED';
     */
}