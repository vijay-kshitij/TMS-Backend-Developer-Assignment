package com.kshitij.tms.repository;

import com.kshitij.tms.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByLoadId(UUID loadId);

}
