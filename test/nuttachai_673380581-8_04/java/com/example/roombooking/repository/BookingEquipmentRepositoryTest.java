package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nuttachai_673380581-8_04-repository;MODE=PostgreSQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("nuttachai_673380581-8_04-test")
class BookingEquipmentRepositoryTest {
    private static final LocalDateTime NINE = LocalDateTime.of(2026, 10, 10, 9, 0);
    private final BookingEquipmentRepository repository;
    private final EntityManager entityManager;
    private User user;
    private MeetingRoom room;
    private Equipment equipment;

    @Autowired
    BookingEquipmentRepositoryTest(BookingEquipmentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void prepareParents() {
        user = new User();
        user.setUsername("fixture-member");
        user.setRole(Role.USER);
        entityManager.persist(user);
        room = new MeetingRoom();
        room.setName("Fixture room");
        room.setCapacity(4);
        room.setRoomType(RoomType.STANDARD);
        room.setStatus(RoomStatus.AVAILABLE);
        entityManager.persist(room);
        equipment = new Equipment();
        equipment.setName("Fixture projector");
        equipment.setTotalQuantity(20);
        entityManager.persist(equipment);
    }

    @Test
    void adjacentReservationsUsePeakQuantityRatherThanSumOfBothIntervals() {
        reserve(BookingStatus.APPROVED, NINE, NINE.plusHours(1), 2);
        reserve(BookingStatus.PENDING, NINE.plusHours(1), NINE.plusHours(2), 3);
        reserve(BookingStatus.CANCELLED, NINE, NINE.plusHours(2), 10);
        reserve(BookingStatus.REJECTED, NINE, NINE.plusHours(2), 10);
        reserve(BookingStatus.COMPLETED, NINE, NINE.plusHours(2), 10);

        assertEquals(3L, repository.sumReservedQuantity(equipment.getId(), NINE, NINE.plusHours(2), null));
        assertEquals(0L, repository.sumReservedQuantity(equipment.getId(), NINE.plusHours(2), NINE.plusHours(3), null));
    }

    @Test
    void overlappingIntervalsAreClippedAndCurrentBookingCanBeExcluded() {
        Booking first = reserve(BookingStatus.APPROVED, NINE, NINE.plusHours(2), 2);
        reserve(BookingStatus.PENDING, NINE.plusHours(1), NINE.plusHours(3), 3);

        assertEquals(5L, repository.sumReservedQuantity(equipment.getId(), NINE.plusMinutes(30), NINE.plusMinutes(150), null));
        assertEquals(3L, repository.sumReservedQuantity(equipment.getId(), NINE, NINE.plusHours(3), first.getId()));
        assertEquals(1, repository.findByBookingId(first.getId()).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void generatedJpaSchemaRejectsNonPositiveQuantities(int quantity) {
        Booking booking = booking(BookingStatus.PENDING, NINE, NINE.plusHours(1));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new BookingEquipment(null, booking, equipment, quantity)));
    }

    @Test
    void replacingLinkedRowsInAnExistingBookingPreservesTheNewQuantity() {
        Booking booking = booking(BookingStatus.PENDING, NINE, NINE.plusHours(1));
        booking.getBookingEquipments().add(new BookingEquipment(null, booking, equipment, 1));
        entityManager.persist(booking);
        entityManager.flush();

        // The existing booking service replaces the collection in this order.
        // IDENTITY can insert the new link before the old orphan is deleted.
        booking.getBookingEquipments().clear();
        booking.getBookingEquipments().add(new BookingEquipment(null, booking, equipment, 2));
        entityManager.merge(booking);
        entityManager.flush();
        entityManager.clear();

        var links = repository.findByBookingId(booking.getId());
        assertEquals(1, links.size());
        assertEquals(2, links.get(0).getQuantity());
    }

    @Test
    void deletingLinksDoesNotDeleteEitherParent() {
        Booking booking = reserve(BookingStatus.PENDING, NINE, NINE.plusHours(1), 1);
        repository.deleteByBookingId(booking.getId());
        repository.flush();
        entityManager.clear();
        assertTrue(repository.findByBookingId(booking.getId()).isEmpty());
        assertNotNull(entityManager.find(Booking.class, booking.getId()));
        assertNotNull(entityManager.find(Equipment.class, equipment.getId()));
    }

    private Booking reserve(BookingStatus status, LocalDateTime start, LocalDateTime end, int quantity) {
        Booking booking = booking(status, start, end);
        repository.saveAndFlush(new BookingEquipment(null, booking, equipment, quantity));
        return booking;
    }

    private Booking booking(BookingStatus status, LocalDateTime start, LocalDateTime end) {
        Booking booking = Booking.builder().user(user).room(room).status(status).startTime(start).endTime(end).build();
        entityManager.persist(booking);
        return booking;
    }
}
