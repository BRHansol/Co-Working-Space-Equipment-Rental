package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.dto.request.EquipmentCreateRequest;
import com.example.roombooking.dto.response.EquipmentResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.EquipmentMapper;
import com.example.roombooking.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceImplTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    private EquipmentServiceImpl equipmentService;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentServiceImpl(equipmentRepository, new EquipmentMapper());
    }

    private EquipmentCreateRequest buildRequest() {
        EquipmentCreateRequest request = new EquipmentCreateRequest();
        request.setName("Projector");
        request.setTotalQuantity(5);
        request.setCategory("Display");
        return request;
    }

    private Equipment buildEquipment(Long id) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName("Projector");
        equipment.setTotalQuantity(5);
        equipment.setCategory("Display");
        return equipment;
    }

    @Test
    void createEquipment_savesAndReturnsResponse() {
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> {
            Equipment saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EquipmentResponse response = equipmentService.createEquipment(buildRequest());

        assertEquals(1L, response.getId());
        assertEquals("Projector", response.getName());
        assertEquals(5, response.getTotalQuantity());
    }

    @Test
    void createEquipment_negativeQuantity_throwsIllegalArgument() {
        EquipmentCreateRequest request = buildRequest();
        request.setTotalQuantity(-1);

        assertThrows(IllegalArgumentException.class, () -> equipmentService.createEquipment(request));
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    void getEquipmentById_found_returnsResponse() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(buildEquipment(1L)));

        EquipmentResponse response = equipmentService.getEquipmentById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Projector", response.getName());
    }

    @Test
    void getEquipmentById_notFound_throwsResourceNotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipmentService.getEquipmentById(99L));
    }

    @Test
    void getAllEquipments_returnsPageOfResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Equipment> page = new PageImpl<>(
                List.of(buildEquipment(1L), buildEquipment(2L)), pageable, 2);
        when(equipmentRepository.findAll(pageable)).thenReturn(page);

        Page<EquipmentResponse> result = equipmentService.getAllEquipments(pageable);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void updateEquipment_updatesFieldsAndSaves() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(buildEquipment(1L)));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentCreateRequest request = buildRequest();
        request.setName("Microphone");
        request.setTotalQuantity(10);

        EquipmentResponse response = equipmentService.updateEquipment(1L, request);

        assertEquals("Microphone", response.getName());
        assertEquals(10, response.getTotalQuantity());
    }

    @Test
    void updateEquipment_notFound_throwsResourceNotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> equipmentService.updateEquipment(99L, buildRequest()));
    }

    @Test
    void updateEquipment_negativeQuantity_throwsIllegalArgument() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(buildEquipment(1L)));
        EquipmentCreateRequest request = buildRequest();
        request.setTotalQuantity(-5);

        assertThrows(IllegalArgumentException.class, () -> equipmentService.updateEquipment(1L, request));
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    void deleteEquipment_found_callsDelete() {
        Equipment existing = buildEquipment(1L);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        equipmentService.deleteEquipment(1L);

        verify(equipmentRepository).delete(existing);
    }

    @Test
    void deleteEquipment_notFound_throwsAndDoesNotDelete() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipmentService.deleteEquipment(99L));
        verify(equipmentRepository, never()).delete(any(Equipment.class));
    }
}
