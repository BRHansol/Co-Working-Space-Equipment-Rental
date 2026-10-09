package com.example.roombooking.controller.api;

import com.example.roombooking.dto.request.EquipmentCreateRequest;
import com.example.roombooking.dto.response.EquipmentResponse;
import com.example.roombooking.exception.ConflictException;
import com.example.roombooking.exception.GlobalExceptionHandler;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ทดสอบ EquipmentController ผ่าน HTTP จริง (MockMvc) พร้อม GlobalExceptionHandler ตัวจริง
 * เช็ก URL /api/v1/equipments, @Valid และ status code 400, 404, 409
 */
@ExtendWith(MockitoExtension.class)
class EquipmentControllerTest {

    private static final String VALID_EQUIPMENT_JSON = """
            {"name":"Projector","totalQuantity":5,"category":"Display"}
            """;

    @Mock
    private EquipmentService equipmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new EquipmentController(equipmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private EquipmentResponse buildResponse(Long id) {
        EquipmentResponse response = new EquipmentResponse();
        response.setId(id);
        response.setName("Projector");
        response.setTotalQuantity(5);
        response.setCategory("Display");
        return response;
    }

    @Test
    void createEquipment_validBody_returns201() throws Exception {
        when(equipmentService.createEquipment(any(EquipmentCreateRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(post("/api/v1/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EQUIPMENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createEquipment_negativeQuantity_returns400AndDoesNotCallService() throws Exception {
        String invalidJson = """
                {"name":"Projector","totalQuantity":-1}
                """;

        mockMvc.perform(post("/api/v1/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(equipmentService, never()).createEquipment(any(EquipmentCreateRequest.class));
    }

    @Test
    void updateEquipment_blankName_returns400AndDoesNotCallService() throws Exception {
        String invalidJson = """
                {"name":"  ","totalQuantity":3}
                """;

        mockMvc.perform(put("/api/v1/equipments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(equipmentService, never()).updateEquipment(eq(1L), any(EquipmentCreateRequest.class));
    }

    @Test
    void getEquipment_notFound_returns404() throws Exception {
        when(equipmentService.getEquipmentById(99L))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: 99"));

        mockMvc.perform(get("/api/v1/equipments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteEquipment_usedInBooking_returns409() throws Exception {
        doThrow(new ConflictException("ไม่สามารถลบอุปกรณ์ได้ เพราะมีการจองที่ใช้อุปกรณ์นี้อยู่ (id: 1)"))
                .when(equipmentService).deleteEquipment(1L);

        mockMvc.perform(delete("/api/v1/equipments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
