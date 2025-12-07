package com.kshitij.tms.controller;

import com.kshitij.tms.dto.BookingRequest;
import com.kshitij.tms.entity.Booking;
import com.kshitij.tms.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // CREATE BOOKING - Accept Bid
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam UUID bidId,
            @RequestBody BookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createBooking(bidId, request));
    }

    // GET BOOKING DETAILS
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingById(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    // CANCEL BOOKING
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}
