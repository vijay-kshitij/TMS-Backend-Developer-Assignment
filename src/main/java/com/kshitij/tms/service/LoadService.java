package com.kshitij.tms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.kshitij.tms.dto.LoadRequest;
import com.kshitij.tms.dto.LoadUpdateRequest;
import com.kshitij.tms.dto.BestBidResponse;
import com.kshitij.tms.entity.Load;
import com.kshitij.tms.entity.LoadStatus;
import com.kshitij.tms.entity.Bid;
import com.kshitij.tms.entity.BidStatus;
import com.kshitij.tms.entity.Transporter;
import com.kshitij.tms.exception.InvalidStatusTransitionException;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.LoadRepository;
import com.kshitij.tms.repository.BidRepository;
import com.kshitij.tms.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class LoadService {

    private final LoadRepository loadRepository;
    private final BidRepository bidRepository;
    private final TransporterRepository transporterRepository;

    /**
     * Create a new load with POSTED status
     */
    public Load createLoad(LoadRequest request) {
        Load load = Load.builder()
                .shipperId(request.getShipperId())
                .loadingCity(request.getLoadingCity())
                .unloadingCity(request.getUnloadingCity())
                .loadingDate(request.getLoadingDate())
                .productType(request.getProductType())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .truckType(request.getTruckType())
                .noOfTrucks(request.getNoOfTrucks())
                .remainingTrucks(request.getNoOfTrucks())  // Initially all trucks are available
                .status(LoadStatus.POSTED)
                .datePosted(LocalDateTime.now())
                .build();

        return loadRepository.save(load);
    }

    /**
     * Get all loads (without filters)
     */
    public List<Load> getAllLoads() {
        return loadRepository.findAll();
    }

    /**
     * Get load by ID
     * @throws ResourceNotFoundException if load doesn't exist
     */
    public Load getLoadById(UUID id) {
        return loadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", id));
    }

    /**
     * Update load details
     * @throws ResourceNotFoundException if load doesn't exist
     */
    public Load updateLoad(UUID id, LoadUpdateRequest request) {
        Load load = getLoadById(id);

        if (request.getLoadingCity() != null) load.setLoadingCity(request.getLoadingCity());
        if (request.getUnloadingCity() != null) load.setUnloadingCity(request.getUnloadingCity());
        if (request.getLoadingDate() != null) load.setLoadingDate(request.getLoadingDate());
        if (request.getProductType() != null) load.setProductType(request.getProductType());
        if (request.getWeight() != null) load.setWeight(request.getWeight());
        if (request.getWeightUnit() != null) load.setWeightUnit(request.getWeightUnit());
        if (request.getTruckType() != null) load.setTruckType(request.getTruckType());
        if (request.getNoOfTrucks() != null) {
            load.setNoOfTrucks(request.getNoOfTrucks());
            load.setRemainingTrucks(request.getNoOfTrucks());
        }

        return loadRepository.save(load);
    }

    /**
     * Cancel a load
     * Business Rules:
     * - Cannot cancel BOOKED loads
     * - Cannot cancel already CANCELLED loads
     * - Rejects all pending bids when cancelled
     *
     * @throws ResourceNotFoundException if load doesn't exist
     * @throws InvalidStatusTransitionException if load is in BOOKED or already CANCELLED status
     */
    @Transactional
    public Load cancelLoad(UUID loadId) {

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        // Rule: Cannot cancel booked loads
        if (load.getStatus() == LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel load in BOOKED status. Load ID: " + loadId);
        }

        // Rule: Cannot cancel already cancelled loads
        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Load is already cancelled. Load ID: " + loadId);
        }

        // 1. Reject all pending bids for this load
        List<Bid> pendingBids = bidRepository.findByLoadIdAndStatus(loadId, BidStatus.PENDING);
        pendingBids.forEach(bid -> bid.setStatus(BidStatus.REJECTED));
        bidRepository.saveAll(pendingBids);

        // 2. Update load status to CANCELLED
        load.setStatus(LoadStatus.CANCELLED);
        return loadRepository.save(load);
    }

    /**
     * Get best bids for a load sorted by score
     * Score formula: (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
     * Higher score = better bid (lower rate + higher rating)
     *
     * @throws ResourceNotFoundException if load doesn't exist
     */
    public List<BestBidResponse> getBestBids(UUID loadId) {

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        // Fetch only PENDING bids (only these are valid choices for acceptance)
        List<Bid> bids = bidRepository.findByLoadIdAndStatus(loadId, BidStatus.PENDING);

        if (bids.isEmpty()) {
            return List.of();
        }

        // Map bids with calculated scores
        List<BestBidResponse> scoredBids = bids.stream()
                .map(bid -> {

                    Transporter transporter = transporterRepository.findById(bid.getTransporterId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Transporter", "transporterId", bid.getTransporterId()));

                    // Best Bid Score Calculation
                    // Lower rate is better (1/rate), higher rating is better
                    double score = (1.0 / bid.getProposedRate()) * 0.7 +
                            (transporter.getRating() / 5.0) * 0.3;

                    return BestBidResponse.builder()
                            .bidId(bid.getBidId())
                            .transporterId(bid.getTransporterId())
                            .proposedRate(bid.getProposedRate())
                            .transporterRating(transporter.getRating())
                            .score(score)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))  // Sort DESC (highest score first)
                .toList();

        return scoredBids;
    }

    /**
     * Filter loads with pagination
     * Supports filtering by shipperId and/or status
     */
    public Page<Load> filterLoads(String shipperId, LoadStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // CASE 1: No filters - return all
        if (shipperId == null && status == null) {
            return loadRepository.findAll(pageable);
        }

        // CASE 2: Only shipperId filter
        if (shipperId != null && status == null) {
            return loadRepository.findByShipperId(shipperId, pageable);
        }

        // CASE 3: Only status filter
        if (shipperId == null && status != null) {
            return loadRepository.findByStatus(status, pageable);
        }

        // CASE 4: Both filters applied
        return loadRepository.findByShipperIdAndStatus(shipperId, status, pageable);
    }

}