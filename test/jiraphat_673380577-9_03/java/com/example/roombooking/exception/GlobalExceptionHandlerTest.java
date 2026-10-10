package com.example.roombooking.exception;

import com.example.roombooking.controller.api.BookingController;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.core.PropertyReferenceException;
import com.example.roombooking.domain.entity.Booking;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ยิง request ผ่าน BookingController จริง แล้วให้ service (mock) โยน exception
// เพื่อเช็คว่า GlobalExceptionHandler แปลงเป็น HTTP status + ErrorResponse ถูกต้อง
@WebMvcTest({BookingController.class, GlobalExceptionHandlerTest.FakeViewController.class})
@Import(GlobalExceptionHandlerTest.FakeViewController.class)
class GlobalExceptionHandlerTest {

    private static final String VALID_BOOKING_JSON = """
            {"roomId": 1, "startTime": "2030-01-01T09:00:00", "endTime": "2030-01-01T10:00:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void resourceNotFound_returns404WithErrorResponse() throws Exception {
        when(bookingService.getBookingById(99L))
                .thenThrow(new ResourceNotFoundException("Booking not found: id=99"));

        mockMvc.perform(get("/api/v1/bookings/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found: id=99"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/99"));
    }

    @Test
    void roomNotAvailable_returns409() throws Exception {
        when(bookingService.createBooking(any(), eq(1L)))
                .thenThrow(new RoomNotAvailableException("Room is already booked"));

        postBooking()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Room is already booked"));
    }

    @Test
    void equipmentNotAvailable_returns409() throws Exception {
        when(bookingService.createBooking(any(), eq(1L)))
                .thenThrow(new EquipmentNotAvailableException("Projector out of stock"));

        postBooking()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Projector out of stock"));
    }

    @Test
    void forbidden_returns403() throws Exception {
        when(bookingService.createBooking(any(), eq(1L)))
                .thenThrow(new ForbiddenException("Only VIP users can book this room"));

        postBooking()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only VIP users can book this room"));
    }

    @Test
    void invalidStateTransition_returns409() throws Exception {
        when(bookingService.updateStatus(5L, BookingStatus.APPROVED, 1L))
                .thenThrow(new InvalidStateTransitionException(BookingStatus.CANCELLED, "approve"));

        mockMvc.perform(patch("/api/v1/bookings/5/status")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot approve a booking in status CANCELLED"));
    }

    @Test
    void conflict_returns409() throws Exception {
        when(bookingService.createBooking(any(), eq(1L)))
                .thenThrow(new ConflictException("Duplicate booking"));

        postBooking()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate booking"));
    }

    @Test
    void dataIntegrityViolation_returns409WithoutLeakingSql() throws Exception {
        when(bookingService.getBookingById(7L)).thenThrow(new DataIntegrityViolationException(
                "violates foreign key constraint \"fk_bookings_user\" on table \"bookings\""));

        mockMvc.perform(get("/api/v1/bookings/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("constraint"))));
    }

    @Test
    void unknownSortProperty_returns400NotMasked500() throws Exception {
        when(bookingService.getBookingsByRoom(eq(1L), any())).thenThrow(new PropertyReferenceException(
                "string", TypeInformation.of(Booking.class), List.of()));

        mockMvc.perform(get("/api/v1/rooms/1/bookings").param("sort", "string"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("string")));
    }

    @Test
    void beanValidationFailure_returns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details", hasItem(containsString("roomId"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("startTime"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("endTime"))));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"));
    }

    @Test
    void unknownEnumValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/5/status")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FINISHED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pathVariableTypeMismatch_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("id")));
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        when(bookingService.updateStatus(5L, BookingStatus.PENDING, 1L))
                .thenThrow(new IllegalArgumentException("Cannot change a booking back to PENDING."));

        mockMvc.perform(patch("/api/v1/bookings/5/status")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot change a booking back to PENDING."));
    }

    // 405 เกิดก่อน Spring เลือก controller ได้ advice ที่จำกัด basePackages เลยไม่ทำงาน
    // แต่ต้องยังได้ 405 ไม่ใช่ 500
    @Test
    void unsupportedHttpMethod_returns405NotMasked500() throws Exception {
        mockMvc.perform(delete("/api/v1/bookings/5"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void controllerOutsideApiPackage_isNotHandledAsJson() {
        // ถ้า advice ไปดัก controller หน้าเว็บด้วย จะได้ 404 + JSON แทนที่ exception จะหลุดไปให้ error page จัดการ
        assertThatThrownBy(() -> mockMvc.perform(get("/test-view/missing")))
                .hasRootCauseInstanceOf(ResourceNotFoundException.class);
    }

    // จำลอง controller หน้า Thymeleaf ที่อยู่นอก package controller.api
    @Controller
    static class FakeViewController {
        @GetMapping("/test-view/missing")
        String missing() {
            throw new ResourceNotFoundException("Booking not found");
        }
    }

    @Test
    void unexpectedException_returns500WithoutLeakingInternals() throws Exception {
        when(bookingService.getBookingById(1L))
                .thenThrow(new RuntimeException("SQL connection password=12345678 refused"));

        mockMvc.perform(get("/api/v1/bookings/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", not(containsString("password"))))
                .andExpect(content().string(not(containsString("RuntimeException"))));
    }

    private org.springframework.test.web.servlet.ResultActions postBooking() throws Exception {
        return mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-Id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOKING_JSON));
    }
}
