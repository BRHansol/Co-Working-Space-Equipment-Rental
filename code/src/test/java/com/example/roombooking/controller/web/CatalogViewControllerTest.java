package com.example.roombooking.controller.web;

import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.controller.web.support.WebViewAdvice;
import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.*;
import com.example.roombooking.dto.response.*;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogViewControllerTest {
    private RoomService rooms;
    private EquipmentService equipment;
    private BookingService bookings;
    private BookingRepository bookingRepository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        rooms = mock(RoomService.class); equipment = mock(EquipmentService.class); bookings = mock(BookingService.class);
        bookingRepository = mock(BookingRepository.class);
        WebSessionSupport sessions = mock(WebSessionSupport.class);
        mvc = MockMvcBuilders.standaloneSetup(new CatalogViewController(rooms, equipment, bookings, bookingRepository, sessions))
                .setControllerAdvice(new WebViewAdvice(sessions)).build();
    }

    @Test
    void dateSearchExcludesMaintenanceAndRoomsWithActiveReservations() throws Exception {
        RoomResponse available = room(1L, RoomStatus.AVAILABLE);
        RoomResponse reserved = room(2L, RoomStatus.AVAILABLE);
        RoomResponse maintenance = room(3L, RoomStatus.MAINTENANCE);
        when(rooms.getAllRooms(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(available, reserved, maintenance)));
        LocalDateTime start = LocalDateTime.of(2027, 1, 12, 9, 0), end = LocalDateTime.of(2027, 1, 12, 11, 0);
        when(bookingRepository.findOverlappingBookings(1L, start, end, List.of(BookingStatus.PENDING, BookingStatus.APPROVED)))
                .thenReturn(List.of());
        when(bookingRepository.findOverlappingBookings(2L, start, end, List.of(BookingStatus.PENDING, BookingStatus.APPROVED)))
                .thenReturn(List.of(new Booking()));
        mvc.perform(get("/rooms").param("date", "2027-01-12").param("startTime", "09:00").param("endTime", "11:00"))
                .andExpect(view().name("rooms/list")).andExpect(model().attribute("rooms", List.of(available)));
        verify(bookingRepository, never()).findOverlappingBookings(eq(3L), any(), any(), anyList());
    }

    @Test
    void invalidIntervalsAndUnknownFiltersReturnBadRequest() throws Exception {
        mvc.perform(get("/rooms").param("date", "2027-01-12").param("startTime", "11:00").param("endTime", "09:00"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/rooms").param("roomType", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/rooms/1").param("startTime", "not-a-time")).andExpect(status().isBadRequest());
        verifyNoInteractions(rooms, bookingRepository);
    }

    @Test
    void publicScheduleIncludesOnlyActiveBookingsOverlappingSelectedDay() throws Exception {
        when(rooms.getRoomById(1L)).thenReturn(room(1L, RoomStatus.AVAILABLE));
        BookingResponse active = booking(41L, BookingStatus.APPROVED, LocalDateTime.of(2027, 1, 11, 18, 0), LocalDateTime.of(2027, 1, 12, 10, 0));
        BookingResponse cancelled = booking(42L, BookingStatus.CANCELLED, LocalDateTime.of(2027, 1, 12, 9, 0), LocalDateTime.of(2027, 1, 12, 11, 0));
        BookingResponse endpointOnly = booking(43L, BookingStatus.PENDING, LocalDateTime.of(2027, 1, 11, 9, 0), LocalDateTime.of(2027, 1, 12, 0, 0));
        when(bookings.getBookingsByRoom(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(active, cancelled, endpointOnly)));
        mvc.perform(get("/rooms/1").param("date", "2027-01-12"))
                .andExpect(view().name("rooms/detail")).andExpect(model().attribute("bookings", List.of(active)));
    }

    @Test
    void missingEquipmentShowsNotFoundPageFromServiceException() throws Exception {
        when(equipment.getEquipmentById(999L)).thenThrow(new ResourceNotFoundException("Equipment not found"));
        mvc.perform(get("/equipment/999")).andExpect(status().isNotFound()).andExpect(view().name("common/error"));
        verify(equipment).getEquipmentById(999L);
        verify(equipment, never()).getAllEquipments(any(Pageable.class));
    }

    @Test
    void existingEquipmentShowsDetailWithOtherEquipmentOnly() throws Exception {
        EquipmentResponse projector = equipmentItem(1L, "Projector");
        EquipmentResponse microphone = equipmentItem(2L, "Microphone");
        EquipmentResponse speaker = equipmentItem(3L, "Speaker");
        when(equipment.getEquipmentById(1L)).thenReturn(projector);
        when(equipment.getAllEquipments(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projector, microphone, speaker)));
        mvc.perform(get("/equipment/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipment/detail"))
                .andExpect(model().attribute("equipment", projector))
                .andExpect(model().attribute("equipmentList", List.of(microphone, speaker)));
        verify(equipment).getEquipmentById(1L);
    }

    private RoomResponse room(Long id, RoomStatus status) {
        RoomResponse room = new RoomResponse(); room.setId(id); room.setName("Focus " + id); room.setCapacity(6);
        room.setFloor("2"); room.setRoomType(RoomType.STANDARD); room.setStatus(status); return room;
    }

    private EquipmentResponse equipmentItem(Long id, String name) {
        EquipmentResponse item = new EquipmentResponse(); item.setId(id); item.setName(name);
        item.setTotalQuantity(5); item.setCategory("AV"); return item;
    }

    private BookingResponse booking(Long id, BookingStatus status, LocalDateTime start, LocalDateTime end) {
        return new BookingResponse(id, 1L, "Focus", 1L, "member", start, end, status, null, List.of(), start.minusDays(1));
    }
}
