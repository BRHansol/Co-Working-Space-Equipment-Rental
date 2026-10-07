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
    void responseContainsLinkedEquipmentDetailsWithoutExposingEntityReferences() {
        Booking booking = mapper.toEntity(request(), room(), user());
        booking.setId(9L);
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

    private BookingCreateRequest request() {
        BookingCreateRequest request = new BookingCreateRequest();
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
