package com.example.roombooking.controller.api;

import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.dto.request.RoomCreateRequest;
import com.example.roombooking.dto.response.RoomResponse;
import com.example.roombooking.exception.ConflictException;
import com.example.roombooking.exception.GlobalExceptionHandler;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.service.RoomService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ทดสอบ RoomController ผ่าน HTTP จริง (MockMvc) พร้อม GlobalExceptionHandler ตัวจริง
 * เพื่อเช็กว่า @Valid / URL /api/v1 / status code 400, 404, 409 ทำงานตามที่ตั้งใจ
 */
@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private static final String VALID_ROOM_JSON = """
            {"name":"Room A","capacity":10,"floor":"2","roomType":"STANDARD","status":"AVAILABLE"}
            """;

    @Mock
    private RoomService roomService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomController(roomService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private RoomResponse buildResponse(Long id) {
        RoomResponse response = new RoomResponse();
        response.setId(id);
        response.setName("Room A");
        response.setCapacity(10);
        response.setFloor("2");
        response.setRoomType(RoomType.STANDARD);
        response.setStatus(RoomStatus.AVAILABLE);
        return response;
    }

    @Test
    void createRoom_validBody_returns201() throws Exception {
        when(roomService.createRoom(any(RoomCreateRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ROOM_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRoom_invalidBody_returns400AndDoesNotCallService() throws Exception {
        // name ว่าง, capacity เป็น 0, ไม่มี roomType/status -> ต้องโดน @Valid ตีกลับ
        String invalidJson = """
                {"name":"","capacity":0}
                """;

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(roomService, never()).createRoom(any(RoomCreateRequest.class));
    }

    @Test
    void getRoom_notFound_returns404() throws Exception {
        when(roomService.getRoomById(99L))
                .thenThrow(new ResourceNotFoundException("Meeting room not found with id: 99"));

        mockMvc.perform(get("/api/v1/rooms/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteRoom_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1"))
                .andExpect(status().isNoContent());

        verify(roomService).deleteRoom(1L);
    }

    @Test
    void deleteRoom_hasBookings_returns409() throws Exception {
        doThrow(new ConflictException("ไม่สามารถลบห้องได้ เพราะมีการจองที่เกี่ยวข้องกับห้องนี้อยู่ (id: 1)"))
                .when(roomService).deleteRoom(1L);

        mockMvc.perform(delete("/api/v1/rooms/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
