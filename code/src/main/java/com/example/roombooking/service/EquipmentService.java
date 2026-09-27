package com.example.roombooking.service;

import com.example.roombooking.domain.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EquipmentService {
    Equipment createEquipment(Equipment equipment);
    Equipment getEquipmentById(Long id);
    Page<Equipment> getAllEquipments(Pageable pageable);
    Equipment updateEquipment(Long id, Equipment equipmentDetails);

    void deleteEquipment(Long id);
}