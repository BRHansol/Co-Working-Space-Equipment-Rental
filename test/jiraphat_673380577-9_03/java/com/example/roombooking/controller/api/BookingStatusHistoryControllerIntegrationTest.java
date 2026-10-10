package com.example.roombooking.controller.api;

import com.example.roombooking.TestData;
import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ครบวง: เปลี่ยนสถานะจริง -> Observer บันทึก history (async หลัง commit) -> อ่านกลับผ่าน REST API
// ไม่ใส่ @Transactional ที่ test เพราะ listener ทำงานแบบ AFTER_COMMIT ต้องให้ commit จริง
@SpringBootTest
@AutoConfigureMockMvc
class BookingStatusHistoryControllerIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeetingRoomRepository roomRepository;
    @Autowired
    private BookingStatusHistoryRepository historyRepository;

    private User admin;
    private Booking booking;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(TestData.user("admin01", Role.ADMIN));
        MeetingRoom room = roomRepository.save(TestData.room("VIP 1"));
        booking = bookingRepository.save(TestData.pendingBooking(admin, room));
    }

    @AfterEach
    void cleanUp() {
        historyRepository.deleteAll();
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void afterApproveAndComplete_historyIsReturnedNewestFirst() throws Exception {
        bookingService.updateStatus(booking.getId(), BookingStatus.APPROVED, admin.getId());
        awaitHistoryCount(1);
        bookingService.updateStatus(booking.getId(), BookingStatus.COMPLETED, null);
        awaitHistoryCount(2);

        mockMvc.perform(get("/api/v1/bookings/{id}/status-history", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].bookingId").value(booking.getId()))
                .andExpect(jsonPath("$[0].oldStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].newStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].changedBy").value("system"))
                .andExpect(jsonPath("$[1].oldStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$[1].changedBy").value("admin01"))
                .andExpect(jsonPath("$[1].changedById").value(admin.getId()))
                .andExpect(jsonPath("$[1].changedAt").isNotEmpty())
                // ไม่ส่งข้อมูลอ่อนไหวของผู้ใช้ออกไป
                .andExpect(jsonPath("$[1].password").doesNotExist());
    }

    @Test
    void bookingWithoutStatusChanges_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{id}/status-history", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownBooking_returns404ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{id}/status-history", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/999999/status-history"));
    }

    @Test
    void nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/abc/status-history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private void awaitHistoryCount(int expected) {
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(historyRepository.findByBooking_IdOrderByChangedAtDesc(booking.getId()))
                        .hasSize(expected));
    }
}
