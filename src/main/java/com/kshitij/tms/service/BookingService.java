package com.kshitij.tms.service;

import com.kshitij.tms.dto.BookingRequest;
import com.kshitij.tms.entity.*;
import com.kshitij.tms.exception.InsufficientCapacityException;
import com.kshitij.tms.exception.InvalidStatusTransitionException;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BidRepository bidRepository;
    private final TransporterRepository transporterRepository;
    private final LoadRepository loadRepository;

    /**
     * Create a booking by accepting a bid
     *
     * Business Rules:
     * 1. Cannot book cancelled loads
     * 2. Verify truck availability before deducting
     * 3. Deduct allocated trucks from transporter's available pool
     * 4. Mark bid as ACCEPTED
     * 5. Reject all other pending bids for this load
     * 6. Update load's remainingTrucks
     * 7. Mark load as BOOKED when remainingTrucks reaches 0
     * 8. Use @Transactional to ensure atomicity
     *
     * @throws ResourceNotFoundException if bid, load, or transporter doesn't exist
     * @throws InvalidStatusTransitionException if load is cancelled
     * @throws InsufficientCapacityException if transporter lacks required trucks
     */
    @Transactional
    public Booking createBooking(UUID bidId, BookingRequest request) {

        // 1. Validate bid exists
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));

        // 2. Validate load exists and is bookable
        Load load = loadRepository.findById(bid.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", bid.getLoadId()));

        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Cannot book CANCELLED load. Load ID: " + load.getLoadId());
        }

        // 3. Validate transporter and check truck availability
        Transporter transporter = transporterRepository.findById(bid.getTransporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transporter", "transporterId", bid.getTransporterId()));

        // Find the specific truck type in transporter's available trucks
        AvailableTruck availableTruck = transporter.getAvailableTrucks().stream()
                .filter(truck -> truck.getTruckType().equalsIgnoreCase(load.getTruckType()))
                .findFirst()
                .orElseThrow(() -> new InsufficientCapacityException(
                        String.format("Transporter %s does not have trucks of type '%s'",
                                transporter.getCompanyName(), load.getTruckType())));

        // Verify sufficient capacity
        if (availableTruck.getCount() < request.getAllocatedTrucks()) {
            throw new InsufficientCapacityException(
                    String.format("Transporter %s only has %d trucks of type '%s' available, but %d requested",
                            transporter.getCompanyName(),
                            availableTruck.getCount(),
                            load.getTruckType(),
                            request.getAllocatedTrucks()));
        }

        // 4. Deduct allocated trucks from transporter's available pool
        availableTruck.setCount(availableTruck.getCount() - request.getAllocatedTrucks());
        transporterRepository.save(transporter);

        // 5. Mark this bid as ACCEPTED
        bid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(bid);

        // 6. Reject all other pending bids for this load
        List<Bid> otherBids = bidRepository.findByLoadId(load.getLoadId());
        otherBids.forEach(otherBid -> {
            if (!otherBid.getBidId().equals(bidId) && otherBid.getStatus() == BidStatus.PENDING) {
                otherBid.setStatus(BidStatus.REJECTED);
                bidRepository.save(otherBid);
            }
        });

        // 7. Create booking entity
        Booking booking = Booking.builder()
                .loadId(bid.getLoadId())
                .bidId(bid.getBidId())
                .transporterId(bid.getTransporterId())
                .allocatedTrucks(request.getAllocatedTrucks())
                .finalRate(request.getFinalRate())
                .truckType(load.getTruckType())
                .status(BookingStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .build();

        bookingRepository.save(booking);

        // 8. Update load's remaining trucks (BUG FIX: was setting noOfTrucks before)
        load.setRemainingTrucks(load.getRemainingTrucks() - request.getAllocatedTrucks());

        // 9. Mark load as BOOKED when all trucks are allocated
        if (load.getRemainingTrucks() == 0) {
            load.setStatus(LoadStatus.BOOKED);
        }

        loadRepository.save(load);

        return booking;
    }

    /**
     * Get booking by ID
     *
     * @throws ResourceNotFoundException if booking doesn't exist
     */
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));
    }

    /**
     * Cancel a booking
     *
     * Business Rules:
     * 1. Cannot cancel already cancelled bookings
     * 2. Restore allocated trucks to transporter's available pool
     * 3. Update load's remainingTrucks
     * 4. If load was BOOKED, change status back to OPEN_FOR_BIDS
     *
     * @throws ResourceNotFoundException if booking, transporter, or load doesn't exist
     * @throws InvalidStatusTransitionException if booking is already cancelled
     */
    @Transactional
    public Booking cancelBooking(UUID bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Booking is already cancelled. Booking ID: " + bookingId);
        }

        // 1. Mark booking as cancelled
        booking.setStatus(BookingStatus.CANCELLED);

        // 2. Restore trucks to transporter's available pool
        Transporter transporter = transporterRepository.findById(booking.getTransporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transporter", "transporterId", booking.getTransporterId()));

        // Find the truck type and restore count
        transporter.getAvailableTrucks().stream()
                .filter(truck -> truck.getTruckType().equalsIgnoreCase(booking.getTruckType()))
                .findFirst()
                .ifPresent(truck -> truck.setCount(truck.getCount() + booking.getAllocatedTrucks()));

        transporterRepository.save(transporter);

        // 3. Update load's remaining trucks
        Load load = loadRepository.findById(booking.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", booking.getLoadId()));

        load.setRemainingTrucks(load.getRemainingTrucks() + booking.getAllocatedTrucks());

        // 4. If load was fully BOOKED, reopen it for bidding
        if (load.getStatus() == LoadStatus.BOOKED) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        }

        loadRepository.save(load);

        return bookingRepository.save(booking);
    }

}