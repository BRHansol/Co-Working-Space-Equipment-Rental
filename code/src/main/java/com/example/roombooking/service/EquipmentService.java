package com.example.roombooking.service;

import com.example.roombooking.dto.request.EquipmentCreateRequest;
import com.example.roombooking.dto.response.EquipmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EquipmentService {
    EquipmentResponse createEquipment(EquipmentCreateRequest request);
    EquipmentResponse getEquipmentById(Long id);
    Page<EquipmentResponse> getAllEquipments(Pageable pageable);
    EquipmentResponse updateEquipment(Long id, EquipmentCreateRequest request);
    void deleteEquipment(Long id);
}