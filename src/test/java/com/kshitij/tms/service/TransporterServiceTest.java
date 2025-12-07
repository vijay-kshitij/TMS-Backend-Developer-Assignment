package com.kshitij.tms.service;

import com.kshitij.tms.dto.TransporterRequest;
import com.kshitij.tms.dto.TransporterUpdateRequest;
import com.kshitij.tms.dto.TruckRequest;
import com.kshitij.tms.entity.AvailableTruck;
import com.kshitij.tms.entity.Transporter;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransporterService
 */
@ExtendWith(MockitoExtension.class)
class TransporterServiceTest {

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private TransporterService transporterService;

    private Transporter testTransporter;
    private UUID transporterId;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();

        AvailableTruck truck1 = AvailableTruck.builder()
                .truckType("Container")
                .count(10)
                .build();

        AvailableTruck truck2 = AvailableTruck.builder()
                .truckType("Flatbed")
                .count(5)
                .build();

        testTransporter = Transporter.builder()
                .transporterId(transporterId)
                .companyName("Fast Logistics")
                .rating(4.5)
                .availableTrucks(Arrays.asList(truck1, truck2))
                .build();
    }

    @Test
    void testRegisterTransporter_Success() {
        // Given
        TruckRequest truckReq1 = TruckRequest.builder()
                .truckType("Container")
                .count(10)
                .build();

        TruckRequest truckReq2 = TruckRequest.builder()
                .truckType("Flatbed")
                .count(5)
                .build();

        TransporterRequest request = TransporterRequest.builder()
                .companyName("Fast Logistics")
                .rating(4.5)
                .availableTrucks(Arrays.asList(truckReq1, truckReq2))
                .build();

        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        // When
        Transporter result = transporterService.createTransporter(request);

        // Then
        assertNotNull(result);
        assertEquals("Fast Logistics", result.getCompanyName());
        assertEquals(4.5, result.getRating());
        assertEquals(2, result.getAvailableTrucks().size());
        verify(transporterRepository, times(1)).save(any(Transporter.class));
    }

    @Test
    void testGetTransporterById_Success() {
        // Given
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));

        // When
        Transporter result = transporterService.getTransporterById(transporterId);

        // Then
        assertNotNull(result);
        assertEquals(transporterId, result.getTransporterId());
        assertEquals("Fast Logistics", result.getCompanyName());
    }

    @Test
    void testGetTransporterById_NotFound() {
        // Given
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            transporterService.getTransporterById(transporterId);
        });
    }

    @Test
    void testUpdateAvailableTrucks_Success() {
        // Given
        TruckRequest truckReq = TruckRequest.builder()
                .truckType("Container")
                .count(20)  // Updated count
                .build();

        TransporterUpdateRequest request = TransporterUpdateRequest.builder()
                .availableTrucks(Arrays.asList(truckReq))
                .build();

        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(testTransporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(testTransporter);

        // When
        Transporter result = transporterService.updateTransporter(transporterId, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAvailableTrucks().size());
        verify(transporterRepository, times(1)).save(testTransporter);
    }

    @Test
    void testGetAllTransporters() {
        // Given
        List<Transporter> transporters = Arrays.asList(testTransporter);
        when(transporterRepository.findAll()).thenReturn(transporters);

        // When
        List<Transporter> result = transporterService.getAllTransporters();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransporter, result.get(0));
    }
}