package com.example.roombooking.mapper;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingMapperTest {
    private final BookingMapper mapper = new BookingMapper();

    @Test
    void newBookingKeepsAnEmptyMutableEquipmentCollectionAndStartsPending() {
        BookingCreateRequest request = request();
        Booking booking = mapper.toEntity(request, room(), user());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(request.getPurpose(), booking.getPurpose());
        assertNotNull(booking.getBookingEquipments());
        assertDoesNotThrow(() -> booking.getBookingEquipments().add(new BookingEquipment()));
    }

    @Test
    void newBookingUsesResolvedReferencesInsteadOfClientIdentifiers() {
        BookingCreateRequest request = request();
        request.setBookingId(555L);
        request.setRoomId(888L);
        request.setBookingForUserId(999L);
        request.setEquipmentItems(List.of(new BookingCreateRequest.EquipmentItemRequest(7L, 2)));
        MeetingRoom resolvedRoom = room();
        User resolvedUser = user();

        Booking booking = mapper.toEntity(request, resolvedRoom, resolvedUser);

        assertNull(booking.getId());
        assertSame(resolvedRoom, booking.getRoom());
        assertSame(resolvedUser, booking.getUser());
        assertEquals(request.getStartTime(), booking.getStartTime());
        assertEquals(request.getEndTime(), booking.getEndTime());
        assertTrue(booking.getBookingEquipments().isEmpty());
    }

    @Test
    void editingDetailsDoesNotReplaceOwnerOrStatus() {
        User owner = user();
        Booking booking = mapper.toEntity(request(), room(), owner);
        booking.setStatus(BookingStatus.APPROVED);
        MeetingRoom replacement = new MeetingRoom();
        replacement.setId(4L);
        BookingCreateRequest change = request();
        change.setBookingForUserId(999L);
        change.setPurpose("Changed details");

        mapper.updateEntity(booking, change, replacement);

        assertSame(owner, booking.getUser());
        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        assertSame(replacement, booking.getRoom());
        assertEquals("Changed details", booking.getPurpose());
    }

    @Test
    void editingDetailsPreservesPersistenceIdentityAndEquipmentCollection() {
        Booking booking = mapper.toEntity(request(), room(), user());
        booking.setId(9L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 8, 0);
        booking.setCreatedAt(createdAt);
        List<BookingEquipment> originalLinks = booking.getBookingEquipments();
        BookingEquipment originalLink = new BookingEquipment(1L, booking, new Equipment(), 2);
        originalLinks.add(originalLink);
        BookingCreateRequest change = request();
        change.setBookingId(999L);
        change.setStartTime(LocalDateTime.of(2026, 10, 11, 23, 30));
        change.setEndTime(change.getStartTime().plusHours(1));
        change.setPurpose(null);
        change.setEquipmentItems(List.of(new BookingCreateRequest.EquipmentItemRequest(8L, 99)));

        mapper.updateEntity(booking, change, room());

        assertEquals(9L, booking.getId());
        assertEquals(createdAt, booking.getCreatedAt());
        assertSame(originalLinks, booking.getBookingEquipments());
        assertEquals(1, originalLinks.size());
        assertSame(originalLink, originalLinks.get(0));
        assertEquals(2, originalLink.getQuantity());
        assertEquals(change.getStartTime(), booking.getStartTime());
        assertEquals(change.getEndTime(), booking.getEndTime());
        assertNull(booking.getPurpose());
    }

    @Test
    void responseContainsLinkedEquipmentDetailsWithoutExposingEntityReferences() {
        Booking booking = mapper.toEntity(request(), room(), user());
        booking.setId(9L);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0, 0, 123456000));
        Equipment equipment = new Equipment();
        equipment.setId(7L);
        equipment.setName("Projector");
        booking.getBookingEquipments().add(new BookingEquipment(1L, booking, equipment, 2));

        var response = mapper.toResponse(booking);

        assertEquals(9L, response.getId());
        assertEquals(3L, response.getRoomId());
        assertEquals("Focus", response.getRoomName());
        assertEquals(1L, response.getUserId());
        assertEquals("member", response.getUsername());
        assertEquals(booking.getStartTime(), response.getStartTime());
        assertEquals(booking.getEndTime(), response.getEndTime());
        assertEquals(BookingStatus.APPROVED, response.getStatus());
        assertEquals(booking.getPurpose(), response.getPurpose());
        assertEquals(booking.getCreatedAt(), response.getCreatedAt());
        assertEquals(1, response.getEquipmentItems().size());
        var item = response.getEquipmentItems().get(0);
        assertEquals(7L, item.getEquipmentId());
        assertEquals("Projector", item.getEquipmentName());
        assertEquals(2, item.getQuantity());
        assertEquals(1, mapper.toResponseList(List.of(booking)).size());
    }

    @Test
    void responseWithoutEquipmentReturnsAnEmptyList() {
        assertTrue(mapper.toResponse(mapper.toEntity(request(), room(), user())).getEquipmentItems().isEmpty());
    }

    @Test
    void responseWithNullEquipmentCollectionReturnsAnEmptyList() {
        Booking booking = mapper.toEntity(request(), room(), user());
        booking.setBookingEquipments(null);

        var response = mapper.toResponse(booking);
        assertNotNull(response.getEquipmentItems());
        assertTrue(response.getEquipmentItems().isEmpty());
    }

    @Test
    void responseRemainsASnapshotAfterBookingAndEquipmentAreChanged() {
        BookingCreateRequest request = request();
        Booking booking = mapper.toEntity(request, room(), user());
        Equipment equipment = new Equipment();
        equipment.setId(7L);
        equipment.setName("Projector");
        BookingEquipment link = new BookingEquipment(1L, booking, equipment, 2);
        booking.getBookingEquipments().add(link);

        var response = mapper.toResponse(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStartTime(booking.getStartTime().plusDays(1));
        booking.setPurpose("Changed purpose");
        equipment.setName("Changed equipment");
        link.setQuantity(5);
        booking.getBookingEquipments().clear();

        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(request.getStartTime(), response.getStartTime());
        assertEquals("Meeting", response.getPurpose());
        assertEquals(1, response.getEquipmentItems().size());
        assertEquals("Projector", response.getEquipmentItems().get(0).getEquipmentName());
        assertEquals(2, response.getEquipmentItems().get(0).getQuantity());
    }

    private BookingCreateRequest request() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomId(3L);
        request.setStartTime(LocalDateTime.of(2026, 10, 10, 9, 0));
        request.setEndTime(request.getStartTime().plusHours(1));
        request.setPurpose("Meeting");
        return request;
    }

    private MeetingRoom room() {
        MeetingRoom room = new MeetingRoom();
        room.setId(3L);
        room.setName("Focus");
        return room;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("member");
        return user;
    }
}
