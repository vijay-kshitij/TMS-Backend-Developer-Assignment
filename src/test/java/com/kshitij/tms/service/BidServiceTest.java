package com.kshitij.tms.service;

import com.kshitij.tms.dto.BidRequest;
import com.kshitij.tms.entity.*;
import com.kshitij.tms.exception.InsufficientCapacityException;
import com.kshitij.tms.exception.InvalidStatusTransitionException;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.BidRepository;
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
 * Unit tests for BidService
 * Tests bid submission, rejection, and business rules
 */
@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private BidService bidService;

    private Load testLoad;
    private Transporter testTransporter;
    private Bid testBid;
    private UUID loadId;
    private UUID transporterId;
    private UUID bidId;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bidId = UUID.randomUUID();

        testLoad = Load.builder()
                .loadId(loadId)
                .shipperId("SHIP123")
                .truckType("Container")
                .status(LoadStatus.POSTED)
                .noOfTrucks(5)
                .remainingTrucks(5)
                .build();

        AvailableTruck truck = AvailableTruck.builder()
                .truckType("Container")
                .count(10)
                .build();

        testTransporter = Transporter.builder()
                .transporterId(transporterId)
                .companyName("Fast Logistics")
                .rating(4.5)
                .availableTrucks(Arrays.asList(truck))
                .build();

        testBid = Bid.builder()
                .bidId(bidId)
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .status(BidStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSubmitBid_Success() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        // When
        Bid result = bidService.submitBid(request);

        // Then
        assertNotNull(result);
        assertEquals(BidStatus.PENDING, result.getStatus());
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    void testSubmitBid_FirstBidChangesLoadStatus() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        testLoad.setStatus(LoadStatus.POSTED);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        // When
        bidService.submitBid(request);

        // Then
        assertEquals(LoadStatus.OPEN_FOR_BIDS, testLoad.getStatus());
        verify(loadRepository, times(1)).save(testLoad);
    }

    @Test
    void testSubmitBid_LoadNotFound() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            bidService.submitBid(request);
        });
    }

    @Test
    void testSubmitBid_CannotBidOnCancelledLoad() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        testLoad.setStatus(LoadStatus.CANCELLED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bidService.submitBid(request);
        });
    }

    @Test
    void testSubmitBid_CannotBidOnBookedLoad() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        testLoad.setStatus(LoadStatus.BOOKED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bidService.submitBid(request);
        });
    }

    @Test
    void testSubmitBid_InsufficientTrucks() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(15)  // More than available (10)
                .build();

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        // When & Then
        assertThrows(InsufficientCapacityException.class, () -> {
            bidService.submitBid(request);
        });
    }

    @Test
    void testSubmitBid_WrongTruckType() {
        // Given
        BidRequest request = BidRequest.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(10000)
                .trucksOffered(3)
                .build();

        testLoad.setTruckType("Flatbed");  // Transporter only has "Container"

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(testLoad));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        // When & Then
        assertThrows(InsufficientCapacityException.class, () -> {
            bidService.submitBid(request);
        });
    }

    @Test
    void testRejectBid_Success() {
        // Given
        testBid.setStatus(BidStatus.PENDING);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));
        when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

        // When
        Bid result = bidService.rejectBid(bidId);

        // Then
        assertEquals(BidStatus.REJECTED, result.getStatus());
        verify(bidRepository, times(1)).save(testBid);
    }

    @Test
    void testRejectBid_CannotRejectNonPendingBid() {
        // Given
        testBid.setStatus(BidStatus.ACCEPTED);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bidService.rejectBid(bidId);
        });
    }

    @Test
    void testRejectBid_NotFound() {
        // Given
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            bidService.rejectBid(bidId);
        });
    }

    @Test
    void testGetBidById_Success() {
        // Given
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        // When
        Bid result = bidService.getBidById(bidId);

        // Then
        assertNotNull(result);
        assertEquals(bidId, result.getBidId());
    }

    @Test
    void testFilterBids_AllFilters() {
        // Given
        List<Bid> bids = Arrays.asList(testBid);
        when(bidRepository.findByLoadIdAndTransporterIdAndStatus(loadId, transporterId, BidStatus.PENDING))
                .thenReturn(bids);

        // When
        List<Bid> result = bidService.filterBids(loadId, transporterId, BidStatus.PENDING);

        // Then
        assertEquals(1, result.size());
        assertEquals(testBid, result.get(0));
    }

    @Test
    void testFilterBids_NoFilters() {
        // Given
        List<Bid> bids = Arrays.asList(testBid);
        when(bidRepository.findAll()).thenReturn(bids);

        // When
        List<Bid> result = bidService.filterBids(null, null, null);

        // Then
        assertEquals(1, result.size());
        verify(bidRepository, times(1)).findAll();
    }
}