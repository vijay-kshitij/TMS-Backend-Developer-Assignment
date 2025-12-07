package com.kshitij.tms.repository;

import com.kshitij.tms.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransporterRepository extends JpaRepository<Transporter, UUID> {

}
