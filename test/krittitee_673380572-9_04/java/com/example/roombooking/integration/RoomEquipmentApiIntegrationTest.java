package com.example.roombooking.integration;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration testing (Week 9): ยิง HTTP ผ่าน Controller -> Service -> Repository -> ฐานข้อมูล H2 จริง
 * ไม่มี mock เลย ใช้ H2 แทน PostgreSQL (Test Double แบบ Fake)
 *
 * ต่างจาก unit test ที่ "จำลอง" ให้ flush() โยน error: ที่นี่ foreign key ชนจริงใน database
 * และเช็กว่า message มี "(id: x)" ซึ่งมาจาก ConflictException ใน service
 * (ถ้า flush() ไม่ทำงาน error จะไปเกิดตอน commit และได้ message กลางของ GlobalExceptionHandler แทน)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Room / Equipment API: Integration test with real H2 database")
class RoomEquipmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MeetingRoomRepository roomRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;
    private final List<Long> bookingIds = new ArrayList<>();
    private final List<Long> roomIds = new ArrayList<>();
    private final List<Long> equipmentIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setUsername("krittitee-it-" + System.nanoTime());
        u.setEmail(u.getUsername() + "@example.com");
        u.setPassword("secret");
        u.setRole(Role.USER);
        user = userRepository.save(u);
    }

    // ลบเฉพาะข้อมูลที่ test นี้สร้าง เพราะ H2 ใช้ร่วมกับ test ของเพื่อนในกลุ่ม
    @AfterEach
    void cleanUp() {
        bookingIds.forEach(id -> bookingRepository.findById(id).ifPresent(bookingRepository::delete));
        roomIds.forEach(id -> roomRepository.findById(id).ifPresent(roomRepository::delete));
        equipmentIds.forEach(id -> equipmentRepository.findById(id).ifPresent(equipmentRepository::delete));
        userRepository.deleteById(user.getId());
    }

    private MeetingRoom saveRoom(String name) {
        MeetingRoom room = new MeetingRoom();
        room.setName(name);
        room.setCapacity(8);
        room.setFloor("3");
        room.setRoomType(RoomType.STANDARD);
        room.setStatus(RoomStatus.AVAILABLE);
        MeetingRoom saved = roomRepository.save(room);
        roomIds.add(saved.getId());
        return saved;
    }

    private Equipment saveEquipment(String name) {
        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setTotalQuantity(5);
        equipment.setCategory("Display");
        Equipment saved = equipmentRepository.save(equipment);
        equipmentIds.add(saved.getId());
        return saved;
    }

    private Booking saveBooking(MeetingRoom room, Equipment equipment) {
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .startTime(start)
                .endTime(start.plusHours(2))
                .status(BookingStatus.PENDING)
                .purpose("Integration test")
                .build();
        if (equipment != null) {
            booking.getBookingEquipments().add(new BookingEquipment(null, booking, equipment, 1));
        }
        Booking saved = bookingRepository.save(booking);
        bookingIds.add(saved.getId());
        return saved;
    }

    @Test
    @DisplayName("TC-IT01: ลบห้องที่ยังมีการจอง -> 409 จาก ConflictException และห้องยังอยู่ใน DB")
    void deleteRoom_withBooking_returns409AndRoomStillExists() throws Exception {
        MeetingRoom room = saveRoom("IT Room with booking");
        saveBooking(room, null);

        mockMvc.perform(delete("/api/v1/rooms/{id}", room.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("(id: " + room.getId() + ")")));

        assertTrue(roomRepository.existsById(room.getId()));
    }

    @Test
    @DisplayName("TC-IT02: ลบห้องที่ไม่มีการจอง -> 204 และห้องหายจาก DB")
    void deleteRoom_withoutBooking_returns204AndRoomIsRemoved() throws Exception {
        MeetingRoom room = saveRoom("IT Room free");

        mockMvc.perform(delete("/api/v1/rooms/{id}", room.getId()))
                .andExpect(status().isNoContent());

        assertFalse(roomRepository.existsById(room.getId()));
    }

    @Test
    @DisplayName("TC-IT03: ลบอุปกรณ์ที่ถูกใช้ในการจอง -> 409 จาก ConflictException และอุปกรณ์ยังอยู่")
    void deleteEquipment_usedInBooking_returns409AndEquipmentStillExists() throws Exception {
        MeetingRoom room = saveRoom("IT Room for equipment");
        Equipment equipment = saveEquipment("IT Projector used");
        saveBooking(room, equipment);

        mockMvc.perform(delete("/api/v1/equipments/{id}", equipment.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("(id: " + equipment.getId() + ")")));

        assertTrue(equipmentRepository.existsById(equipment.getId()));
    }

    @Test
    @DisplayName("TC-IT04: ลบอุปกรณ์ที่ไม่ถูกใช้ -> 204 และอุปกรณ์หายจาก DB")
    void deleteEquipment_unused_returns204AndEquipmentIsRemoved() throws Exception {
        Equipment equipment = saveEquipment("IT Projector free");

        mockMvc.perform(delete("/api/v1/equipments/{id}", equipment.getId()))
                .andExpect(status().isNoContent());

        assertFalse(equipmentRepository.existsById(equipment.getId()));
    }

    @Test
    @DisplayName("TC-IT05: ดูห้องที่ไม่มีอยู่ผ่าน stack จริง -> 404")
    void getRoom_notExisting_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("TC-IT06: สร้างห้องด้วยข้อมูลถูกต้อง -> 201 และบันทึกลง DB จริง")
    void createRoom_validBody_returns201AndPersists() throws Exception {
        String body = """
                {"name":"IT Created Room","capacity":12,"floor":"5","roomType":"VIP","status":"AVAILABLE"}
                """;

        String response = mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("IT Created Room"))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(response.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        roomIds.add(id);
        assertTrue(roomRepository.existsById(id));
    }
}
