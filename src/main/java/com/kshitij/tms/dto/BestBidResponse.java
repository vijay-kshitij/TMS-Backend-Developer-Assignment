package com.kshitij.tms.dto;

import lombok.*;

import java.util.UUID;

/**
 * Response DTO for best bid recommendations
 * Contains bid details with calculated score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestBidResponse {

    private UUID bidId;
    private UUID transporterId;
    private double proposedRate;
    private double transporterRating;
    private double score;  // Calculated score: (1/rate)*0.7 + (rating/5)*0.3
}