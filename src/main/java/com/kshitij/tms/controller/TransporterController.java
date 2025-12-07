package com.kshitij.tms.controller;

import com.kshitij.tms.dto.TransporterRequest;
import com.kshitij.tms.entity.Transporter;
import com.kshitij.tms.service.TransporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import com.kshitij.tms.dto.TransporterUpdateRequest;


import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/transporters")
@RequiredArgsConstructor
public class TransporterController {

    private final TransporterService transporterService;

    @PostMapping
    public ResponseEntity<Transporter> createTransporter(
            @Valid @RequestBody TransporterRequest request
    ) {
        return ResponseEntity.ok(transporterService.createTransporter(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transporter> getTransporterById(@PathVariable UUID id) {
        return ResponseEntity.ok(transporterService.getTransporterById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transporter> updateTransporter(
            @PathVariable UUID id,
            @Valid @RequestBody TransporterUpdateRequest request
    ) {
        return ResponseEntity.ok(transporterService.updateTransporter(id, request));
    }


}
