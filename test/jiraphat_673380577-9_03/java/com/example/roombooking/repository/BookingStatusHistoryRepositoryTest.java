package com.example.roombooking.repository;

import com.example.roombooking.TestData;
import com.example.roombooking.config.JpaAuditingConfig;
import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BookingStatusHistoryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private BookingStatusHistoryRepository historyRepository;

    private User admin;
    private Booking booking;

    @BeforeEach
    void setUp() {
        admin = em.persist(TestData.user("admin", Role.ADMIN));
        MeetingRoom room = em.persist(TestData.room("Room A"));
        booking = em.persist(TestData.pendingBooking(admin, room));
    }

    @Test
    void save_fillsChangedAtThroughJpaAuditing() {
        BookingStatusHistory saved = historyRepository.saveAndFlush(
                history(booking, BookingStatus.PENDING, BookingStatus.APPROVED, admin));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChangedAt()).isNotNull()
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void save_persistsRelationsAndStatuses() {
        Long id = historyRepository.saveAndFlush(
                history(booking, BookingStatus.PENDING, BookingStatus.REJECTED, admin)).getId();
        em.clear();

        BookingStatusHistory found = historyRepository.findById(id).orElseThrow();
        assertThat(found.getBooking().getId()).isEqualTo(booking.getId());
        assertThat(found.getChangedBy().getId()).isEqualTo(admin.getId());
        assertThat(found.getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(found.getNewStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void statuses_areStoredAsStringsNotOrdinals() {
        Long id = historyRepository.saveAndFlush(
                history(booking, BookingStatus.PENDING, BookingStatus.APPROVED, admin)).getId();

        Object stored = em.getEntityManager()
                .createNativeQuery("SELECT new_status FROM booking_status_history WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(stored).isEqualTo("APPROVED");
    }

    @Test
    void findByBookingId_returnsOnlyThatBookingsHistoryNewestFirst() {
        BookingStatusHistory first = history(booking, BookingStatus.PENDING, BookingStatus.APPROVED, admin);
        BookingStatusHistory second = history(booking, BookingStatus.APPROVED, BookingStatus.COMPLETED, null);
        historyRepository.saveAndFlush(first);
        historyRepository.saveAndFlush(second);
        // changed_at เป็น updatable = false เลยตั้งเวลาผ่าน SQL ตรง ๆ ให้ลำดับแน่นอน ไม่ขึ้นกับความเร็วเครื่อง
        setChangedAt(first.getId(), LocalDateTime.now().minusMinutes(5));
        setChangedAt(second.getId(), LocalDateTime.now());

        Booking otherBooking = em.persist(TestData.pendingBooking(admin, booking.getRoom()));
        historyRepository.saveAndFlush(history(otherBooking, BookingStatus.PENDING, BookingStatus.CANCELLED, admin));
        em.flush();
        em.clear();

        List<BookingStatusHistory> result = historyRepository.findByBooking_IdOrderByChangedAtDesc(booking.getId());

        assertThat(result).extracting(BookingStatusHistory::getNewStatus)
                .containsExactly(BookingStatus.COMPLETED, BookingStatus.APPROVED);
    }

    private void setChangedAt(Long historyId, LocalDateTime changedAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE booking_status_history SET changed_at = :changedAt WHERE id = :id")
                .setParameter("changedAt", changedAt)
                .setParameter("id", historyId)
                .executeUpdate();
    }

    private static BookingStatusHistory history(Booking booking, BookingStatus oldStatus,
                                                BookingStatus newStatus, User changedBy) {
        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(booking);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        return history;
    }
}
