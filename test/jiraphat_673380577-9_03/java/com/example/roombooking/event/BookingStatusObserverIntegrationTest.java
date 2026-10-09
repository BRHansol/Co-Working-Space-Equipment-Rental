package com.example.roombooking.event;

import com.example.roombooking.TestData;
import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.exception.InvalidStateTransitionException;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

// ทดสอบ Observer ครบวง: BookingService (State Pattern) -> publish event -> NotificationListener -> history ลง DB
// ไม่ใส่ @Transactional ที่ test เพราะ listener ทำงานแบบ AFTER_COMMIT ต้องให้ commit จริง
@SpringBootTest
@RecordApplicationEvents
class BookingStatusObserverIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

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
    @Autowired
    private ApplicationEvents events;

    private User admin;
    private Booking booking;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(TestData.user("admin", Role.ADMIN));
        MeetingRoom room = roomRepository.save(TestData.room("Room A"));
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
    void approve_publishesEventAndRecordsHistory() {
        bookingService.updateStatus(booking.getId(), BookingStatus.APPROVED, admin.getId());

        List<BookingStatusChangedEvent> published = events.stream(BookingStatusChangedEvent.class).toList();
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(published.get(0).getNewStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(published.get(0).getChangedBy().getId()).isEqualTo(admin.getId());

        List<BookingStatusHistory> history = awaitHistoryCount(1);
        assertThat(history.get(0).getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(history.get(0).getNewStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(history.get(0).getChangedAt()).isNotNull();
    }

    @Test
    void everyTransition_addsOneHistoryRow() {
        bookingService.updateStatus(booking.getId(), BookingStatus.APPROVED, admin.getId());
        awaitHistoryCount(1);
        bookingService.updateStatus(booking.getId(), BookingStatus.COMPLETED, null);

        List<BookingStatusHistory> history = awaitHistoryCount(2);
        assertThat(history).extracting(BookingStatusHistory::getNewStatus)
                .containsExactlyInAnyOrder(BookingStatus.APPROVED, BookingStatus.COMPLETED);
    }

    @Test
    void invalidTransition_publishesNothingAndRecordsNoHistory() {
        bookingService.updateStatus(booking.getId(), BookingStatus.CANCELLED, admin.getId());
        awaitHistoryCount(1);
        events.clear();

        assertThatThrownBy(() ->
                bookingService.updateStatus(booking.getId(), BookingStatus.APPROVED, admin.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThat(events.stream(BookingStatusChangedEvent.class)).isEmpty();
        // รอสักพักให้แน่ใจว่าไม่มี listener async ตัวไหนแอบเขียน history เพิ่ม
        await().during(Duration.ofMillis(500)).atMost(TIMEOUT).untilAsserted(() ->
                assertThat(historyRepository.findByBooking_IdOrderByChangedAtDesc(booking.getId())).hasSize(1));
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    private List<BookingStatusHistory> awaitHistoryCount(int expected) {
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(historyRepository.findByBooking_IdOrderByChangedAtDesc(booking.getId()))
                        .hasSize(expected));
        return historyRepository.findByBooking_IdOrderByChangedAtDesc(booking.getId());
    }
}
