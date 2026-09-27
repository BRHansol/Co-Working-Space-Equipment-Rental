package com.example.roombooking.controller.api;

import com.example.roombooking.dto.request.EquipmentCreateRequest;
import com.example.roombooking.dto.response.EquipmentResponse;
import com.example.roombooking.service.EquipmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@RequestBody EquipmentCreateRequest request) {
        return new ResponseEntity<>(equipmentService.createEquipment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    @GetMapping
    public ResponseEntity<Page<EquipmentResponse>> getAllEquipments(Pageable pageable) {
        return ResponseEntity.ok(equipmentService.getAllEquipments(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> updateEquipment(@PathVariable Long id, @RequestBody EquipmentCreateRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}