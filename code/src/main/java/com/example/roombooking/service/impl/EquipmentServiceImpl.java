package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.service.EquipmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentServiceImpl(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    @Transactional
    public Equipment createEquipment(Equipment equipment) {
        if (equipment.getTotalQuantity() != null && equipment.getTotalQuantity() < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative");
        }
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    @Override
    public Page<Equipment> getAllEquipments(Pageable pageable) {
        return equipmentRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Equipment updateEquipment(Long id, Equipment equipmentDetails) {
        Equipment existingEquipment = getEquipmentById(id);

        if (equipmentDetails.getTotalQuantity() != null && equipmentDetails.getTotalQuantity() < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative");
        }

        existingEquipment.setName(equipmentDetails.getName());
        existingEquipment.setTotalQuantity(equipmentDetails.getTotalQuantity());
        existingEquipment.setCategory(equipmentDetails.getCategory());

        return equipmentRepository.save(existingEquipment);
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment existingEquipment = getEquipmentById(id);
        equipmentRepository.delete(existingEquipment);
    }
}