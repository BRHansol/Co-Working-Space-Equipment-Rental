package com.example.roombooking.service.validation;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.request.BookingCreateRequest.EquipmentItemRequest;
import com.example.roombooking.exception.EquipmentNotAvailableException;
import com.example.roombooking.exception.ForbiddenException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.exception.RoomNotAvailableException;
import com.example.roombooking.repository.BookingEquipmentRepository;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.MeetingRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingValidationTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 9, 0);
    private static final List<BookingStatus> ACTIVE = List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    @Test
    void mergesRepeatedEquipmentBeforeCheckingStock() {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        BookingEquipmentRepository reservations = mock(BookingEquipmentRepository.class);
        Equipment projector = new Equipment();
        projector.setName("Projector");
        projector.setTotalQuantity(5);
        BookingValidationContext context = context();
        context.getRequest().setBookingId(99L);
        context.getRequest().setEquipmentItems(List.of(item(7L, 2), item(7L, 1)));
        when(equipment.findById(7L)).thenReturn(Optional.of(projector));
        when(reservations.sumReservedQuantity(7L, START, START.plusHours(1), 99L)).thenReturn(2L);

        new EquipmentAvailabilityHandler(equipment, reservations).handle(context);

        assertEquals(3, context.getRequestedEquipmentQuantities().get(7L));
        verify(equipment, times(1)).findById(7L);
        verify(reservations).sumReservedQuantity(7L, START, START.plusHours(1), 99L);
    }

    @Test
    void rejectsMergedQuantityOverflowBeforeQueryingInventory() {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        BookingEquipmentRepository reservations = mock(BookingEquipmentRepository.class);
        BookingValidationContext context = context();
        context.getRequest().setEquipmentItems(List.of(item(7L, Integer.MAX_VALUE), item(7L, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAvailabilityHandler(equipment, reservations).handle(context));
        verifyNoInteractions(equipment, reservations);
    }

    static Stream<EquipmentItemRequest> malformedItems() {
        return Stream.of(null, item(null, 1), item(7L, null), item(7L, 0), item(7L, -1));
    }

    @ParameterizedTest
    @MethodSource("malformedItems")
    void rejectsMalformedEquipmentBeforeQueryingInventory(EquipmentItemRequest item) {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        BookingEquipmentRepository reservations = mock(BookingEquipmentRepository.class);
        BookingValidationContext context = context();
        context.getRequest().setEquipmentItems(Arrays.asList(item));

        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAvailabilityHandler(equipment, reservations).handle(context));
        verifyNoInteractions(equipment, reservations);
    }

    @Test
    void permitsBookingWithoutEquipment() {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        BookingEquipmentRepository reservations = mock(BookingEquipmentRepository.class);
        EquipmentAvailabilityHandler handler = new EquipmentAvailabilityHandler(equipment, reservations);
        BookingValidationContext context = context();
        assertDoesNotThrow(() -> handler.handle(context));
        context.getRequest().setEquipmentItems(List.of());
        assertDoesNotThrow(() -> handler.handle(context));
        verifyNoInteractions(equipment, reservations);
    }

    @Test
    void rejectsQuantityAboveRemainingStockAndMissingEquipment() {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        BookingEquipmentRepository reservations = mock(BookingEquipmentRepository.class);
        Equipment projector = new Equipment();
        projector.setName("Projector");
        projector.setTotalQuantity(5);
        BookingValidationContext context = context();
        context.getRequest().setEquipmentItems(List.of(item(7L, 3)));
        when(equipment.findById(7L)).thenReturn(Optional.of(projector));
        when(reservations.sumReservedQuantity(7L, START, START.plusHours(1), null)).thenReturn(3L);
        EquipmentAvailabilityHandler handler = new EquipmentAvailabilityHandler(equipment, reservations);
        assertThrows(EquipmentNotAvailableException.class, () -> handler.handle(context));

        when(equipment.findById(7L)).thenReturn(Optional.empty());
        BookingValidationContext missing = context();
        missing.getRequest().setEquipmentItems(List.of(item(7L, 1)));
        assertThrows(ResourceNotFoundException.class, () -> handler.handle(missing));
    }

    @Test
    void memberCannotBookForSomeoneElseOrUseSuspendedAccount() {
        BookingValidationContext context = context();
        context.getRequest().setBookingForUserId(2L);
        UserPermissionHandler handler = new UserPermissionHandler();
        assertThrows(ForbiddenException.class, () -> handler.handle(context));
        context.getRequest().setBookingForUserId(1L);
        assertDoesNotThrow(() -> handler.handle(context));
        context.getRequester().setActive(false);
        assertThrows(ForbiddenException.class, () -> handler.handle(context));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "STAFF"})
    void staffAndAdminMayBookForAnotherUser(Role role) {
        BookingValidationContext context = context();
        context.getRequester().setRole(role);
        context.getRequest().setBookingForUserId(2L);
        assertDoesNotThrow(() -> new UserPermissionHandler().handle(context));
    }

    @Test
    void validatesRoomBeforePuttingItInContext() {
        MeetingRoomRepository rooms = mock(MeetingRoomRepository.class);
        RoomAvailabilityHandler handler = new RoomAvailabilityHandler(rooms);
        BookingValidationContext context = context();
        context.getRequest().setRoomId(null);
        assertThrows(IllegalArgumentException.class, () -> handler.handle(context));
        verifyNoInteractions(rooms);
        context.getRequest().setRoomId(3L);
        when(rooms.findById(3L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> handler.handle(context));
        MeetingRoom room = new MeetingRoom();
        room.setName("Room");
        room.setStatus(RoomStatus.MAINTENANCE);
        when(rooms.findById(3L)).thenReturn(Optional.of(room));
        assertThrows(RoomNotAvailableException.class, () -> handler.handle(context));
        assertNull(context.getRoom());
        room.setStatus(RoomStatus.AVAILABLE);
        handler.handle(context);
        assertSame(room, context.getRoom());
    }

    @Test
    void rejectsMissingEqualAndReversedTimesBeforeQuery() {
        BookingRepository bookings = mock(BookingRepository.class);
        TimeOverlapHandler handler = new TimeOverlapHandler(bookings);
        BookingValidationContext context = context();
        context.getRequest().setEndTime(START);
        assertThrows(IllegalArgumentException.class, () -> handler.handle(context));
        context.getRequest().setEndTime(START.minusHours(1));
        assertThrows(IllegalArgumentException.class, () -> handler.handle(context));
        context.getRequest().setEndTime(null);
        assertThrows(IllegalArgumentException.class, () -> handler.handle(context));
        verifyNoInteractions(bookings);
    }

    @Test
    void excludesCurrentBookingButRejectsAnotherActiveBooking() {
        BookingRepository bookings = mock(BookingRepository.class);
        TimeOverlapHandler handler = new TimeOverlapHandler(bookings);
        BookingValidationContext context = context();
        context.getRequest().setBookingId(9L);
        Booking current = Booking.builder().id(9L).build();
        Booking another = Booking.builder().id(10L).build();
        when(bookings.findOverlappingBookings(3L, START, START.plusHours(1), ACTIVE))
                .thenReturn(List.of(current));
        assertDoesNotThrow(() -> handler.handle(context));
        when(bookings.findOverlappingBookings(3L, START, START.plusHours(1), ACTIVE))
                .thenReturn(List.of(current, another));
        assertThrows(RoomNotAvailableException.class, () -> handler.handle(context));
    }

    @Test
    void chainStopsBeforeLaterHandlersAfterValidationFails() {
        BookingValidationHandler first = mock(BookingValidationHandler.class, CALLS_REAL_METHODS);
        BookingValidationHandler rejected = mock(BookingValidationHandler.class, CALLS_REAL_METHODS);
        BookingValidationHandler later = mock(BookingValidationHandler.class, CALLS_REAL_METHODS);
        BookingValidationContext context = context();
        doThrow(new ForbiddenException("denied")).when(rejected).doValidate(context);

        assertThrows(ForbiddenException.class,
                () -> new BookingValidationChain(List.of(first, rejected, later)).validate(context));
        var order = inOrder(first, rejected);
        order.verify(first).doValidate(context);
        order.verify(rejected).doValidate(context);
        verifyNoInteractions(later);
    }

    @Test
    void invalidContextFailsAtTheBoundary() {
        assertThrows(IllegalArgumentException.class, () -> new BookingValidationContext(null, new User()));
        assertThrows(IllegalArgumentException.class, () -> new BookingValidationContext(new BookingCreateRequest(), null));
        assertThrows(IllegalArgumentException.class, () -> new BookingValidationChain(List.of()).validate(null));
    }

    private static EquipmentItemRequest item(Long id, Integer quantity) {
        return new EquipmentItemRequest(id, quantity);
    }

    private static BookingValidationContext context() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomId(3L);
        request.setStartTime(START);
        request.setEndTime(START.plusHours(1));
        User requester = new User();
        requester.setId(1L);
        requester.setRole(Role.USER);
        return new BookingValidationContext(request, requester);
    }
}
