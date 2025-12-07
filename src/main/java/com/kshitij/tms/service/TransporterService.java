package com.kshitij.tms.service;

import com.kshitij.tms.dto.TransporterRequest;
import com.kshitij.tms.dto.TransporterUpdateRequest;
import com.kshitij.tms.dto.TruckRequest;
import com.kshitij.tms.entity.AvailableTruck;
import com.kshitij.tms.entity.Transporter;
import com.kshitij.tms.exception.ResourceNotFoundException;
import com.kshitij.tms.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransporterService {

    private final TransporterRepository transporterRepository;

    /**
     * Register a new transporter with available trucks
     */
    public Transporter createTransporter(TransporterRequest request) {

        // Convert TruckRequest DTOs to AvailableTruck entities
        List<AvailableTruck> trucks = request.getAvailableTrucks().stream()
                .map(truckReq -> AvailableTruck.builder()
                        .truckType(truckReq.getTruckType())
                        .count(truckReq.getCount())
                        .build())
                .collect(Collectors.toList());

        Transporter transporter = Transporter.builder()
                .companyName(request.getCompanyName())
                .rating(request.getRating())
                .availableTrucks(trucks)
                .build();

        return transporterRepository.save(transporter);
    }

    /**
     * Get transporter by ID
     *
     * @throws ResourceNotFoundException if transporter doesn't exist
     */
    public Transporter getTransporterById(UUID transporterId) {
        return transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transporter", "transporterId", transporterId));
    }

    /**
     * Update available trucks for a transporter
     * This replaces the entire truck list with the new one
     *
     * @throws ResourceNotFoundException if transporter doesn't exist
     */
    public Transporter updateTransporter(UUID transporterId, TransporterUpdateRequest request) {

        Transporter transporter = getTransporterById(transporterId);

        // Convert TruckRequest DTOs to AvailableTruck entities
        List<AvailableTruck> updatedTrucks = request.getAvailableTrucks().stream()
                .map(truckReq -> AvailableTruck.builder()
                        .truckType(truckReq.getTruckType())
                        .count(truckReq.getCount())
                        .build())
                .collect(Collectors.toList());

        transporter.setAvailableTrucks(updatedTrucks);

        return transporterRepository.save(transporter);
    }

    /**
     * Get all transporters
     */
    public List<Transporter> getAllTransporters() {
        return transporterRepository.findAll();
    }
}