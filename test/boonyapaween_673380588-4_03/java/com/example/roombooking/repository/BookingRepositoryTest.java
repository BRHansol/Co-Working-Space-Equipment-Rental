package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Booking repository queries against a real (H2) database:
 * the overlap query used by TimeOverlapHandler and room/user pagination with sorting.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:boonyapaween_673380588-4_03-repository;MODE=PostgreSQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("boonyapaween_673380588-4_03-test")
class BookingRepositoryTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2030, 1, 1, 9, 0);
    private static final List<BookingStatus> ACTIVE = List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    private User sol;
    private User friend;
    private MeetingRoom roomA;
    private MeetingRoom roomB;

    @BeforeEach
    void prepareParents() {
        sol = persistUser("sol");
        friend = persistUser("friend");
        roomA = persistRoom("Room A");
        roomB = persistRoom("Room B");
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(Role.USER);
        entityManager.persist(user);
        return user;
    }

    private MeetingRoom persistRoom(String name) {
        MeetingRoom room = new MeetingRoom();
        room.setName(name);
        room.setCapacity(8);
        room.setRoomType(RoomType.STANDARD);
        room.setStatus(RoomStatus.AVAILABLE);
        entityManager.persist(room);
        return room;
    }

    private Booking book(MeetingRoom room, User user, BookingStatus status, LocalDateTime start, LocalDateTime end) {
        return bookingRepository.saveAndFlush(Booking.builder()
                .room(room)
                .user(user)
                .status(status)
                .startTime(start)
                .endTime(end)
                .purpose("fixture")
                .build());
    }

    // ---------- findOverlappingBookings ----------

    @Test
    void partiallyOverlappingActiveBooking_isFound() {
        Booking existing = book(roomA, sol, BookingStatus.APPROVED, NINE, NINE.plusHours(2));

        List<Booking> result = bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE.plusHours(1), NINE.plusHours(3), ACTIVE);

        assertThat(result).extracting(Booking::getId).containsExactly(existing.getId());
    }

    @Test
    void bookingFullyInsideOrAroundRequestedSlot_isFound() {
        Booking inside = book(roomA, sol, BookingStatus.PENDING, NINE.plusMinutes(30), NINE.plusMinutes(60));

        assertThat(bookingRepository.findOverlappingBookings(roomA.getId(), NINE, NINE.plusHours(2), ACTIVE))
                .extracting(Booking::getId).containsExactly(inside.getId());
        assertThat(bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE.plusMinutes(40), NINE.plusMinutes(50), ACTIVE))
                .extracting(Booking::getId).containsExactly(inside.getId());
    }

    @Test
    void backToBackBookings_doNotOverlap() {
        book(roomA, sol, BookingStatus.APPROVED, NINE, NINE.plusHours(1));

        assertThat(bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE.plusHours(1), NINE.plusHours(2), ACTIVE)).isEmpty();
        assertThat(bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE.minusHours(1), NINE, ACTIVE)).isEmpty();
    }

    @Test
    void cancelledRejectedAndCompletedBookings_areIgnored() {
        book(roomA, sol, BookingStatus.CANCELLED, NINE, NINE.plusHours(2));
        book(roomA, sol, BookingStatus.REJECTED, NINE, NINE.plusHours(2));
        book(roomA, sol, BookingStatus.COMPLETED, NINE, NINE.plusHours(2));

        assertThat(bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE, NINE.plusHours(2), ACTIVE)).isEmpty();
    }

    @Test
    void bookingInAnotherRoom_isIgnored() {
        book(roomB, sol, BookingStatus.APPROVED, NINE, NINE.plusHours(2));

        assertThat(bookingRepository.findOverlappingBookings(
                roomA.getId(), NINE, NINE.plusHours(2), ACTIVE)).isEmpty();
    }

    // ---------- pagination & sorting ----------

    @Test
    void findByRoomId_pagesAndSortsByStartTimeDescending() {
        Booking first = book(roomA, sol, BookingStatus.APPROVED, NINE, NINE.plusHours(1));
        Booking second = book(roomA, friend, BookingStatus.PENDING, NINE.plusDays(1), NINE.plusDays(1).plusHours(1));
        Booking third = book(roomA, sol, BookingStatus.PENDING, NINE.plusDays(2), NINE.plusDays(2).plusHours(1));
        book(roomB, sol, BookingStatus.PENDING, NINE.plusDays(3), NINE.plusDays(3).plusHours(1));

        Page<Booking> firstPage = bookingRepository.findByRoomId(
                roomA.getId(), PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "startTime")));
        Page<Booking> secondPage = bookingRepository.findByRoomId(
                roomA.getId(), PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "startTime")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Booking::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(secondPage.getContent()).extracting(Booking::getId)
                .containsExactly(first.getId());
    }

    @Test
    void findByUserId_returnsOnlyThatUsersBookings() {
        Booking mine = book(roomA, sol, BookingStatus.PENDING, NINE, NINE.plusHours(1));
        book(roomB, friend, BookingStatus.PENDING, NINE, NINE.plusHours(1));

        Page<Booking> page = bookingRepository.findByUserId(sol.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Booking::getId).containsExactly(mine.getId());
    }
}