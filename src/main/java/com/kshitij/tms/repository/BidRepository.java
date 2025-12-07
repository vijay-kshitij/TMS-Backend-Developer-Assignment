package com.kshitij.tms.repository;

import com.kshitij.tms.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import com.kshitij.tms.entity.BidStatus;


public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByLoadId(UUID loadId);
    List<Bid> findByTransporterId(UUID transporterId);
    List<Bid> findByStatus(BidStatus status);

    List<Bid> findByLoadIdAndTransporterId(UUID loadId, UUID transporterId);
    List<Bid> findByLoadIdAndStatus(UUID loadId, BidStatus status);
    List<Bid> findByTransporterIdAndStatus(UUID transporterId, BidStatus status);

    List<Bid> findByLoadIdAndTransporterIdAndStatus(UUID loadId, UUID transporterId, BidStatus status);

    boolean existsByLoadIdAndTransporterId(UUID loadId, UUID transporterId);
}
