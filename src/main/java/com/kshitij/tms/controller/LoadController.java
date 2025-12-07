package com.kshitij.tms.controller;

import org.springframework.data.domain.Page;

import com.kshitij.tms.dto.LoadRequest;
import com.kshitij.tms.dto.LoadUpdateRequest;
import com.kshitij.tms.dto.BestBidResponse;
import com.kshitij.tms.entity.Load;
import com.kshitij.tms.entity.LoadStatus;
import com.kshitij.tms.service.LoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loads")
@RequiredArgsConstructor
public class LoadController {

    private final LoadService loadService;

    @PostMapping
    public ResponseEntity<Load> createLoad(@Valid @RequestBody LoadRequest request) {
        return ResponseEntity.ok(loadService.createLoad(request));
    }

    @GetMapping
    public Page<Load> filterLoads(
            @RequestParam(required = false) String shipperId,
            @RequestParam(required = false) LoadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return loadService.filterLoads(shipperId, status, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Load> getLoadById(@PathVariable UUID id) {
        return ResponseEntity.ok(loadService.getLoadById(id));
    }

    @PatchMapping("/{loadId}/cancel")
    public ResponseEntity<Load> cancelLoad(@PathVariable UUID loadId) {
        return ResponseEntity.ok(loadService.cancelLoad(loadId));
    }

    @GetMapping("/{loadId}/best-bids")
    public ResponseEntity<List<BestBidResponse>> getBestBids(@PathVariable UUID loadId) {
        return ResponseEntity.ok(loadService.getBestBids(loadId));
    }


}
