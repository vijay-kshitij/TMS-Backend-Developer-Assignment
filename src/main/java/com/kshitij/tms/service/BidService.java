package com.kshitij.tms.service;

import com.kshitij.tms.dto.BidRequest;
import com.kshitij.tms.entity.*;
import com.kshitij.tms.exception.InsufficientCapacityException;
import com.kshitij.tms.exception.InvalidStatusTransitionException;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.BidRepository;
import com.kshitij.tms.repository.LoadRepository;
import com.kshitij.tms.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final LoadRepository loadRepository;
    private final TransporterRepository transporterRepository;

    /**
     * Submit a bid for a load
     *
     * Business Rules:
     * 1. Cannot bid on CANCELLED or BOOKED loads
     * 2. Transporter must have sufficient trucks of the required type
     * 3. First bid changes load status from POSTED to OPEN_FOR_BIDS
     *
     * @throws ResourceNotFoundException if load or transporter doesn't exist
     * @throws InvalidStatusTransitionException if load is CANCELLED or BOOKED
     * @throws InsufficientCapacityException if transporter lacks required trucks
     */
    @Transactional
    public Bid submitBid(BidRequest request) {

        // Validate load exists
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", request.getLoadId()));

        // Rule: Cannot bid on CANCELLED or BOOKED loads
        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Cannot bid on CANCELLED load. Load ID: " + request.getLoadId());
        }

        if (load.getStatus() == LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException(
                    "Cannot bid on BOOKED load. Load ID: " + request.getLoadId());
        }

        // Validate transporter exists
        Transporter transporter = transporterRepository.findById(request.getTransporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transporter", "transporterId", request.getTransporterId()));

        // Rule: Transporter must have enough trucks of the required type
        boolean hasSufficientTrucks = transporter.getAvailableTrucks().stream()
                .anyMatch(truck ->
                        truck.getTruckType().equalsIgnoreCase(load.getTruckType()) &&
                                truck.getCount() >= request.getTrucksOffered()
                );

        if (!hasSufficientTrucks) {
            throw new InsufficientCapacityException(
                    String.format("Transporter %s does not have %d trucks of type '%s'. Available trucks: %s",
                            transporter.getCompanyName(),
                            request.getTrucksOffered(),
                            load.getTruckType(),
                            transporter.getAvailableTrucks())
            );
        }

        // Status Transition Rule: First bid changes POSTED → OPEN_FOR_BIDS
        if (load.getStatus() == LoadStatus.POSTED) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            loadRepository.save(load);
        }

        // Create and save bid
        Bid bid = Bid.builder()
                .loadId(request.getLoadId())
                .transporterId(request.getTransporterId())
                .proposedRate(request.getProposedRate())
                .trucksOffered(request.getTrucksOffered())
                .status(BidStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        return bidRepository.save(bid);
    }

    /**
     * Reject a bid
     *
     * @throws ResourceNotFoundException if bid doesn't exist
     * @throws InvalidStatusTransitionException if bid is not in PENDING status
     */
    public Bid rejectBid(UUID bidId) {

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Only PENDING bids can be rejected. Current status: " + bid.getStatus());
        }

        bid.setStatus(BidStatus.REJECTED);
        // Note: Updated timestamp to reflect rejection time
        bid.setSubmittedAt(LocalDateTime.now());

        return bidRepository.save(bid);
    }

    /**
     * Get bid by ID
     *
     * @throws ResourceNotFoundException if bid doesn't exist
     */
    public Bid getBidById(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));
    }

    /**
     * Get all bids for a specific load
     */
    public List<Bid> getBidsForLoad(UUID loadId) {
        return bidRepository.findByLoadId(loadId);
    }

    /**
     * Filter bids with multiple optional parameters
     * Supports combinations of loadId, transporterId, and status filters
     */
    public List<Bid> filterBids(UUID loadId, UUID transporterId, BidStatus status) {

        // All three filters
        if (loadId != null && transporterId != null && status != null) {
            return bidRepository.findByLoadIdAndTransporterIdAndStatus(loadId, transporterId, status);
        }

        // Two filter combinations
        if (loadId != null && transporterId != null) {
            return bidRepository.findByLoadIdAndTransporterId(loadId, transporterId);
        }

        if (loadId != null && status != null) {
            return bidRepository.findByLoadIdAndStatus(loadId, status);
        }

        if (transporterId != null && status != null) {
            return bidRepository.findByTransporterIdAndStatus(transporterId, status);
        }

        // Single filter
        if (loadId != null) {
            return bidRepository.findByLoadId(loadId);
        }

        if (transporterId != null) {
            return bidRepository.findByTransporterId(transporterId);
        }

        if (status != null) {
            return bidRepository.findByStatus(status);
        }

        // No filters - return all bids
        return bidRepository.findAll();
    }

}