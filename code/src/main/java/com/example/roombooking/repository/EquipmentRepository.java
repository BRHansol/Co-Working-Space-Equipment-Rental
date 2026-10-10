package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}