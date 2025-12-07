package com.kshitij.tms.controller;

import com.kshitij.tms.dto.BidRequest;
import com.kshitij.tms.entity.Bid;
import com.kshitij.tms.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
import com.kshitij.tms.entity.BidStatus;


@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<Bid> submitBid(@Valid @RequestBody BidRequest request) {
        return ResponseEntity.ok(bidService.submitBid(request));
    }

    @GetMapping("/{bidId}")
    public ResponseEntity<Bid> getBidById(@PathVariable UUID bidId) {
        return ResponseEntity.ok(bidService.getBidById(bidId));
    }

    @GetMapping
    public ResponseEntity<List<Bid>> filterBids(
            @RequestParam(required = false) UUID loadId,
            @RequestParam(required = false) UUID transporterId,
            @RequestParam(required = false) BidStatus status
    ) {
        return ResponseEntity.ok(bidService.filterBids(loadId, transporterId, status));
    }

    @PatchMapping("/{bidId}/reject")
    public ResponseEntity<Bid> rejectBid(@PathVariable UUID bidId) {
        return ResponseEntity.ok(bidService.rejectBid(bidId));
    }


}
