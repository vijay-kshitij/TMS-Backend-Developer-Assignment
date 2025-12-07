package com.kshitij.tms.service;

import com.kshitij.tms.dto.BookingRequest;
import com.kshitij.tms.entity.*;
import com.kshitij.tms.exception.InsufficientCapacityException;
import com.kshitij.tms.exception.InvalidStatusTransitionException;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.BidRepository;
import com.kshitij.tms.repository.BookingRepository;
import com.kshitij.tms.repository.LoadRepository;
import com.kshitij.tms.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService
 * Tests booking creation, cancellation, and critical business rules
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private BookingService bookingService;

    private Load testLoad;
    private Bid testBid;
    private Transporter testTransporter;
    private Booking testBooking;
    private AvailableTruck availableTruck;

    private UUID loadId;
    private UUID bidId;
    private UUID transporterId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        testLoad = Load.builder()
                .loadId(loadId)
                .shipperId("SHIP123")
                .truckType("Container")
                .status(LoadStatus.OPEN_FOR_BIDS)
                .noOfTrucks(5)
                .remainingTrucks(5)
                .build();

        testBid = Bid.builder()
                .bidId(bidId)
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .status(BidStatus.PENDING)
                .build();

        availableTruck = AvailableTruck.builder()
                .truckType("Container")
                .count(10)
                .build();

        testTransporter = Transporter.builder()
                .transporterId(transporterId)
                .companyName("Fast Logistics")
                .rating(4.5)
                .availableTrucks(new ArrayList<>(Arrays.asList(availableTruck)))
                .build();

        testBooking = Booking.builder()
                .bookingId(bookingId)
                .loadId(loadId)
                .bidId(bidId)
                .transporterId(transporterId)
                .allocatedTrucks(3)
                .finalRate(10000)
                .truckType("Container")
                .status(BookingStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateBooking_Success() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(3)
                .finalRate(10000)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bidRepository.findByLoadId(loadId)).thenReturn(Arrays.asList(testBid));

        // When
        Booking result = bookingService.createBooking(bidId, request);

        // Then
        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(3, result.getAllocatedTrucks());

        // Verify trucks were deducted
        assertEquals(7, availableTruck.getCount());  // 10 - 3 = 7

        // Verify remaining trucks updated
        assertEquals(2, testLoad.getRemainingTrucks());  // 5 - 3 = 2

        // Verify bid marked as ACCEPTED
        verify(bidRepository, times(1)).save(testBid);
        assertEquals(BidStatus.ACCEPTED, testBid.getStatus());
    }

    @Test
    void testCreateBooking_LoadFullyBooked() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(5)  // All remaining trucks
                .finalRate(10000)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bidRepository.findByLoadId(loadId)).thenReturn(Arrays.asList(testBid));

        // When
        bookingService.createBooking(bidId, request);

        // Then
        assertEquals(0, testLoad.getRemainingTrucks());
        assertEquals(LoadStatus.BOOKED, testLoad.getStatus());
    }

    @Test
    void testCreateBooking_BidNotFound() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(3)
                .finalRate(10000)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.createBooking(bidId, request);
        });
    }

    @Test
    void testCreateBooking_LoadCancelled() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(3)
                .finalRate(10000)
                .build();

        testLoad.setStatus(LoadStatus.CANCELLED);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.createBooking(bidId, request);
        });
    }

    @Test
    void testCreateBooking_InsufficientTrucks() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(15)  // More than available (10)
                .finalRate(10000)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        // When & Then
        assertThrows(InsufficientCapacityException.class, () -> {
            bookingService.createBooking(bidId, request);
        });
    }

    @Test
    void testCreateBooking_TruckTypeNotAvailable() {
        // Given
        BookingRequest request = BookingRequest.builder()
                .allocatedTrucks(3)
                .finalRate(10000)
                .build();

        testLoad.setTruckType("Flatbed");  // Transporter only has "Container"

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        // When & Then
        assertThrows(InsufficientCapacityException.class, () -> {
            bookingService.createBooking(bidId, request);
        });
    }

    @Test
    void testCancelBooking_Success() {
        // Given
        testLoad.setStatus(LoadStatus.BOOKED);
        testLoad.setRemainingTrucks(0);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        Booking result = bookingService.cancelBooking(bookingId);

        // Then
        assertEquals(BookingStatus.CANCELLED, result.getStatus());

        // Verify trucks were restored
        assertEquals(13, availableTruck.getCount());  // 10 + 3 = 13

        // Verify load status changed to OPEN_FOR_BIDS
        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus());

        // Verify remaining trucks increased
        assertEquals(3, testLoad.getRemainingTrucks());  // 0 + 3 = 3
    }

    @Test
    void testCancelBooking_AlreadyCancelled() {
        // Given
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.cancelBooking(bookingId);
        });
    }

    @Test
    void testCancelBooking_NotFound() {
        // Given
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.cancelBooking(bookingId);
        });
    }

    @Test
    void testGetBookingById_Success() {
        // Given
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        // When
        Booking result = bookingService.getBookingById(bookingId);

        // Then
        assertNotNull(result);
        assertEquals(bookingId, result.getBookingId());
    }

    @Test
    void testGetBookingById_NotFound() {
        // Given
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.getBookingById(bookingId);
        });
    }
}