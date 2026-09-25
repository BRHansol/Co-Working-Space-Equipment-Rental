package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.dto.request.EquipmentCreateRequest;
import com.example.roombooking.dto.response.EquipmentResponse;
import com.example.roombooking.mapper.EquipmentMapper;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.service.EquipmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentServiceImpl(EquipmentRepository equipmentRepository, EquipmentMapper equipmentMapper) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentMapper = equipmentMapper;
    }

    @Override
    @Transactional
    public EquipmentResponse createEquipment(EquipmentCreateRequest request) {
        if (request.getTotalQuantity() != null && request.getTotalQuantity() < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative");
        }
        Equipment equipment = equipmentMapper.toEntity(request);
        Equipment savedEquipment = equipmentRepository.save(equipment);
        return equipmentMapper.toResponse(savedEquipment);
    }

    @Override
    public EquipmentResponse getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        return equipmentMapper.toResponse(equipment);
    }

    @Override
    public Page<EquipmentResponse> getAllEquipments(Pageable pageable) {
        return equipmentRepository.findAll(pageable)
                .map(equipmentMapper::toResponse);
    }

    @Override
    @Transactional
    public EquipmentResponse updateEquipment(Long id, EquipmentCreateRequest request) {
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));

        if (request.getTotalQuantity() != null && request.getTotalQuantity() < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative");
        }

        existingEquipment.setName(request.getName());
        existingEquipment.setTotalQuantity(request.getTotalQuantity());
        existingEquipment.setCategory(request.getCategory());

        Equipment updatedEquipment = equipmentRepository.save(existingEquipment);
        return equipmentMapper.toResponse(updatedEquipment);
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        equipmentRepository.delete(existingEquipment);
    }
}