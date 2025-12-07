package com.kshitij.tms.service;

import com.kshitij.tms.dto.LoadRequest;
import com.kshitij.tms.dto.BestBidResponse;
import com.kshitij.tms.entity.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoadService
 * Tests business logic without database dependencies
 */
@ExtendWith(MockitoExtension.class)
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private LoadService loadService;

    private Load testLoad;
    private UUID testLoadId;

    @BeforeEach
    void setUp() {
        testLoadId = UUID.randomUUID();
        testLoad = Load.builder()
                .loadId(testLoadId)
                .shipperId("SHIP123")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .loadingDate(LocalDateTime.now().plusDays(1))
                .productType("Electronics")
                .weight(1000)
                .weightUnit(WeightUnit.KG)
                .truckType("Container")
                .noOfTrucks(5)
                .remainingTrucks(5)
                .status(LoadStatus.POSTED)
                .datePosted(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateLoad_Success() {
        // Given
        LoadRequest request = LoadRequest.builder()
                .shipperId("SHIP123")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .loadingDate(LocalDateTime.now().plusDays(1))
                .productType("Electronics")
                .weight(1000)
                .weightUnit(WeightUnit.KG)
                .truckType("Container")
                .noOfTrucks(5)
                .build();

        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // When
        Load result = loadService.createLoad(request);

        // Then
        assertNotNull(result);
        assertEquals(LoadStatus.POSTED, result.getStatus());
        assertEquals(5, result.getRemainingTrucks());
        assertEquals(5, result.getNoOfTrucks());
        verify(loadRepository, times(1)).save(any(Load.class));
    }

    @Test
    void testGetLoadById_Success() {
        // Given
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));

        // When
        Load result = loadService.getLoadById(testLoadId);

        // Then
        assertNotNull(result);
        assertEquals(testLoadId, result.getLoadId());
        verify(loadRepository, times(1)).findById(testLoadId);
    }

    @Test
    void testGetLoadById_NotFound() {
        // Given
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            loadService.getLoadById(testLoadId);
        });
    }

    @Test
    void testCancelLoad_Success() {
        // Given
        testLoad.setStatus(LoadStatus.OPEN_FOR_BIDS);
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(bidRepository.findByLoadIdAndStatus(testLoadId, BidStatus.PENDING))
                .thenReturn(new ArrayList<>());
        when(loadRepository.save(any(Load.class))).thenReturn(testLoad);

        // When
        Load result = loadService.cancelLoad(testLoadId);

        // Then
        assertEquals(LoadStatus.CANCELLED, result.getStatus());
        verify(loadRepository, times(1)).save(testLoad);
    }

    @Test
    void testCancelLoad_CannotCancelBookedLoad() {
        // Given
        testLoad.setStatus(LoadStatus.BOOKED);
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            loadService.cancelLoad(testLoadId);
        });
    }

    @Test
    void testCancelLoad_AlreadyCancelled() {
        // Given
        testLoad.setStatus(LoadStatus.CANCELLED);
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));

        // When & Then
        assertThrows(InvalidStatusTransitionException.class, () -> {
            loadService.cancelLoad(testLoadId);
        });
    }

    @Test
    void testGetBestBids_Success() {
        // Given
        UUID transporterId1 = UUID.randomUUID();
        UUID transporterId2 = UUID.randomUUID();

        Transporter transporter1 = Transporter.builder()
                .transporterId(transporterId1)
                .companyName("Fast Logistics")
                .rating(4.5)
                .build();

        Transporter transporter2 = Transporter.builder()
                .transporterId(transporterId2)
                .companyName("Quick Transport")
                .rating(3.8)
                .build();

        Bid bid1 = Bid.builder()
                .bidId(UUID.randomUUID())
                .loadId(testLoadId)
                .transporterId(transporterId1)
                .proposedRate(10000)
                .status(BidStatus.PENDING)
                .build();

        Bid bid2 = Bid.builder()
                .bidId(UUID.randomUUID())
                .loadId(testLoadId)
                .transporterId(transporterId2)
                .proposedRate(9000)
                .status(BidStatus.PENDING)
                .build();

        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(bidRepository.findByLoadIdAndStatus(testLoadId, BidStatus.PENDING))
                .thenReturn(Arrays.asList(bid1, bid2));
        when(transporterRepository.findById(transporterId1)).thenReturn(Optional.of(transporter1));
        when(transporterRepository.findById(transporterId2)).thenReturn(Optional.of(transporter2));

        // When
        List<BestBidResponse> result = loadService.getBestBids(testLoadId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // First bid should have higher score (lower rate + higher rating)
        assertTrue(result.get(0).getScore() >= result.get(1).getScore());
    }

    @Test
    void testGetBestBids_NoBids() {
        // Given
        when(loadRepository.findById(testLoadId)).thenReturn(Optional.of(testLoad));
        when(bidRepository.findByLoadIdAndStatus(testLoadId, BidStatus.PENDING))
                .thenReturn(new ArrayList<>());

        // When
        List<BestBidResponse> result = loadService.getBestBids(testLoadId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterLoads_WithShipperIdAndStatus() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        List<Load> loads = Arrays.asList(testLoad);
        Page<Load> page = new PageImpl<>(loads, pageable, loads.size());

        when(loadRepository.findByShipperIdAndStatus("SHIP123", LoadStatus.POSTED, pageable))
                .thenReturn(page);

        // When
        Page<Load> result = loadService.filterLoads("SHIP123", LoadStatus.POSTED, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testLoad, result.getContent().get(0));
    }

    @Test
    void testFilterLoads_NoFilters() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        List<Load> loads = Arrays.asList(testLoad);
        Page<Load> page = new PageImpl<>(loads, pageable, loads.size());

        when(loadRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Load> result = loadService.filterLoads(null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(loadRepository, times(1)).findAll(pageable);
    }
}