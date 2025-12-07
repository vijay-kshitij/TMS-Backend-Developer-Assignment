package com.kshitij.tms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kshitij.tms.entity.Load;
import com.kshitij.tms.entity.LoadStatus;

import java.util.UUID;

public interface LoadRepository extends JpaRepository<Load, UUID> {

    Page<Load> findByShipperId(String shipperId, Pageable pageable);

    Page<Load> findByStatus(LoadStatus status, Pageable pageable);

    Page<Load> findByShipperIdAndStatus(String shipperId, LoadStatus status, Pageable pageable);
}

