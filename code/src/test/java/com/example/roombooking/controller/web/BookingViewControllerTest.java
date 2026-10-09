package com.example.roombooking.controller.web;

import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.controller.web.support.WebViewAdvice;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import com.example.roombooking.dto.response.EquipmentResponse;
import com.example.roombooking.dto.response.RoomResponse;
import com.example.roombooking.exception.EquipmentNotAvailableException;
import com.example.roombooking.service.BookingService;
import com.example.roombooking.service.EquipmentService;
import com.example.roombooking.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingViewControllerTest {
    private BookingService bookings;
    private RoomService rooms;
    private EquipmentService equipment;
    private WebSessionSupport sessions;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private User actor;
    private RoomResponse room;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingService.class);
        rooms = mock(RoomService.class);
        equipment = mock(EquipmentService.class);
        sessions = mock(WebSessionSupport.class);
        actor = new User();
        actor.setId(1L);
        actor.setUsername("narin");
        actor.setRole(Role.USER);
        when(sessions.requireUser(any())).thenReturn(actor);
        when(sessions.getCurrentUser(any())).thenReturn(actor);
        when(sessions.csrfToken(any())).thenReturn("test-csrf");

        room = new RoomResponse();
        room.setId(2L);
        room.setName("Focus room");
        room.setCapacity(6);
        room.setFloor("2");
        room.setRoomType(RoomType.STANDARD);
        room.setStatus(RoomStatus.AVAILABLE);
        when(rooms.getRoomById(2L)).thenReturn(room);
        when(rooms.getAllRooms(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(room)));
        EquipmentResponse projector = new EquipmentResponse();
        projector.setId(7L);
        projector.setName("Projector");
        projector.setTotalQuantity(5);
        when(equipment.getAllEquipments(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(projector)));

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new BookingViewController(bookings, rooms, equipment, sessions))
                .setControllerAdvice(new WebViewAdvice(sessions)).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void wizardBindsEquipmentAndSendsServerOwnerAcrossMultipleDays() throws Exception {
        MockHttpSession session = new MockHttpSession();
        saveDetails(session).andExpect(redirectedUrl("/bookings/new/equipment"));
        mvc.perform(post("/bookings/new/equipment").session(session).param("quantities[7]", "2")
                .param("bookingForUserId", "999").param("bookingId", "999"))
                .andExpect(redirectedUrl("/bookings/new/review"));
        when(bookings.createBooking(any(), eq(1L))).thenReturn(booking(41L, 1L, BookingStatus.PENDING));
        mvc.perform(post("/bookings/new/submit").session(session).param("bookingForUserId", "999"))
                .andExpect(redirectedUrl("/bookings/41/submitted"));

        ArgumentCaptor<BookingCreateRequest> dto = ArgumentCaptor.forClass(BookingCreateRequest.class);
        verify(bookings).createBooking(dto.capture(), eq(1L));
        assertEquals(2L, dto.getValue().getRoomId());
        assertNull(dto.getValue().getBookingForUserId());
        assertNull(dto.getValue().getBookingId());
        assertEquals(LocalDateTime.of(2027, 1, 12, 9, 0), dto.getValue().getStartTime());
        assertEquals(LocalDateTime.of(2027, 1, 13, 11, 0), dto.getValue().getEndTime());
        assertEquals("Team workshop", dto.getValue().getPurpose());
        assertEquals(1, dto.getValue().getEquipmentItems().size());
        assertEquals(7L, dto.getValue().getEquipmentItems().get(0).getEquipmentId());
        assertEquals(2, dto.getValue().getEquipmentItems().get(0).getQuantity());
    }

    @Test
    void duplicateSubmissionCreatesOnlyOneBooking() throws Exception {
        MockHttpSession session = readyDraft();
        when(bookings.createBooking(any(), eq(1L))).thenReturn(booking(41L, 1L, BookingStatus.PENDING));
        mvc.perform(post("/bookings/new/submit").session(session)).andExpect(redirectedUrl("/bookings/41/submitted"));
        mvc.perform(post("/bookings/new/submit").session(session)).andExpect(redirectedUrl("/bookings/41/submitted"));
        verify(bookings, times(1)).createBooking(any(), eq(1L));
    }

    @Test
    void finalAvailabilityFailurePreservesDraftAndAllowsRetry() throws Exception {
        MockHttpSession session = readyDraft();
        when(bookings.createBooking(any(), eq(1L)))
                .thenThrow(new EquipmentNotAvailableException("อุปกรณ์ไม่พอในช่วงที่เลือก"))
                .thenReturn(booking(41L, 1L, BookingStatus.PENDING));
        mvc.perform(post("/bookings/new/submit").session(session))
                .andExpect(redirectedUrl("/bookings/new/review"))
                .andExpect(flash().attribute("flashError", "อุปกรณ์ไม่พอในช่วงที่เลือก"));
        mvc.perform(get("/bookings/new/review").session(session)).andExpect(view().name("bookings/review"));
        mvc.perform(post("/bookings/new/submit").session(session)).andExpect(redirectedUrl("/bookings/41/submitted"));
    }

    @Test
    void cannotSkipWizardOrReuseAnotherUsersDraft() throws Exception {
        mvc.perform(post("/bookings/new/submit").session(new MockHttpSession()))
                .andExpect(redirectedUrl("/bookings/new"));
        MockHttpSession session = readyDraft();
        User other = new User();
        other.setId(99L);
        when(sessions.requireUser(any())).thenReturn(other);
        mvc.perform(post("/bookings/new/submit").session(session)).andExpect(redirectedUrl("/bookings/new"));
        verify(bookings, never()).createBooking(any(), anyLong());
    }

    @Test
    void zeroEquipmentIsOptionalAndExcludedFromCreateRequest() throws Exception {
        MockHttpSession session = new MockHttpSession();
        saveDetails(session);
        mvc.perform(post("/bookings/new/equipment").session(session).param("quantities[7]", "0"))
                .andExpect(redirectedUrl("/bookings/new/review"));
        when(bookings.createBooking(any(), eq(1L))).thenReturn(booking(41L, 1L, BookingStatus.PENDING));
        mvc.perform(post("/bookings/new/submit").session(session)).andExpect(status().is3xxRedirection());
        ArgumentCaptor<BookingCreateRequest> dto = ArgumentCaptor.forClass(BookingCreateRequest.class);
        verify(bookings).createBooking(dto.capture(), eq(1L));
        assertTrue(dto.getValue().getEquipmentItems().isEmpty());
    }

    @Test
    void invalidEquipmentQuantitiesStayOnFormAndDoNotReachPersistence() throws Exception {
        for (String quantity : List.of("-1", "6", "not-a-number")) {
            MockHttpSession session = new MockHttpSession();
            saveDetails(session);
            mvc.perform(post("/bookings/new/equipment").session(session).param("quantities[7]", quantity))
                    .andExpect(view().name("bookings/equipment")).andExpect(model().attributeExists("formErrors"));
            mvc.perform(post("/bookings/new/submit").session(session)).andExpect(redirectedUrl("/bookings/new/equipment"));
        }
        MockHttpSession session = new MockHttpSession();
        saveDetails(session);
        mvc.perform(post("/bookings/new/equipment").session(session).param("quantities[999]", "1"))
                .andExpect(view().name("bookings/equipment"));
        verify(bookings, never()).createBooking(any(), anyLong());
    }

    @Test
    void invalidDateOrderAndMaintenanceRoomAreRejectedBeforeDraftIsSaved() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/bookings/new/details").session(session).param("roomId", "2")
                        .param("date", "2027-01-12").param("endDate", "2027-01-11")
                        .param("startTime", "09:00").param("endTime", "11:00"))
                .andExpect(view().name("bookings/details")).andExpect(model().attributeHasFieldErrors("detailsForm", "endTime"));
        room.setStatus(RoomStatus.MAINTENANCE);
        saveDetails(session).andExpect(view().name("bookings/details"))
                .andExpect(model().attributeHasFieldErrors("detailsForm", "roomId"));
        mvc.perform(get("/bookings/new/equipment").session(session)).andExpect(redirectedUrl("/bookings/new"));
    }

    @Test
    void otherUserCannotReadEditOrCancelBooking() throws Exception {
        when(bookings.getBookingById(42L)).thenReturn(booking(42L, 99L, BookingStatus.PENDING));
        mvc.perform(get("/bookings/42")).andExpect(status().isForbidden());
        mvc.perform(get("/bookings/42/edit")).andExpect(status().isForbidden());
        mvc.perform(post("/bookings/42/edit")).andExpect(status().isForbidden());
        mvc.perform(post("/bookings/42/cancel")).andExpect(status().isForbidden());
        verify(bookings, never()).updateBooking(anyLong(), any(), anyLong());
        verify(bookings, never()).updateStatus(anyLong(), any(), anyLong());
    }

    @Test
    void managerCanAccessBookingAndCancelApprovedStateWithSessionActor() throws Exception {
        when(sessions.isManager(actor)).thenReturn(true);
        when(bookings.getBookingById(42L)).thenReturn(booking(42L, 99L, BookingStatus.APPROVED));
        mvc.perform(get("/bookings/42")).andExpect(view().name("bookings/detail"));
        mvc.perform(post("/bookings/42/cancel").param("actorId", "999"))
                .andExpect(redirectedUrl("/bookings/42"));
        verify(bookings).updateStatus(42L, BookingStatus.CANCELLED, 1L);
    }

    @Test
    void approvedCannotBeEditedAndCancelledCannotBeCancelledAgain() throws Exception {
        when(bookings.getBookingById(42L)).thenReturn(booking(42L, 1L, BookingStatus.APPROVED));
        mvc.perform(get("/bookings/42/edit")).andExpect(redirectedUrl("/bookings/42"));
        mvc.perform(post("/bookings/42/edit")).andExpect(redirectedUrl("/bookings/42"));
        when(bookings.getBookingById(42L)).thenReturn(booking(42L, 1L, BookingStatus.CANCELLED));
        mvc.perform(post("/bookings/42/cancel")).andExpect(redirectedUrl("/bookings/42"));
        verify(bookings, never()).updateBooking(anyLong(), any(), anyLong());
        verify(bookings, never()).updateStatus(anyLong(), any(), anyLong());
    }

    @Test
    void pendingEditSendsActualChangesAndPreservesServerOwnership() throws Exception {
        when(bookings.getBookingById(42L)).thenReturn(booking(42L, 1L, BookingStatus.PENDING));
        mvc.perform(post("/bookings/42/edit").param("roomId", "2").param("date", "2027-01-12")
                        .param("startTime", "10:00").param("endTime", "12:00").param("quantities[7]", "3")
                        .param("bookingForUserId", "999").param("bookingId", "999"))
                .andExpect(redirectedUrl("/bookings/42")).andExpect(flash().attributeExists("flashSuccess"));
        ArgumentCaptor<BookingCreateRequest> dto = ArgumentCaptor.forClass(BookingCreateRequest.class);
        verify(bookings).updateBooking(eq(42L), dto.capture(), eq(1L));
        assertNull(dto.getValue().getBookingForUserId());
        assertNull(dto.getValue().getBookingId());
        assertEquals(LocalDateTime.of(2027, 1, 12, 12, 0), dto.getValue().getEndTime());
        assertEquals(3, dto.getValue().getEquipmentItems().get(0).getQuantity());
    }

    @Test
    void listFiltersOnlyCurrentUsersBookingsAndHandlesDateMaximum() throws Exception {
        BookingResponse pending = booking(41L, 1L, BookingStatus.PENDING);
        BookingResponse approved = booking(42L, 1L, BookingStatus.APPROVED);
        when(bookings.getBookingsByUser(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending, approved)));
        mvc.perform(get("/bookings").param("status", "PENDING").param("to", "+999999999-12-31"))
                .andExpect(view().name("bookings/list")).andExpect(model().attribute("bookings", List.of(pending)));
        verify(bookings).getBookingsByUser(eq(1L), any(Pageable.class));
    }

    private org.springframework.test.web.servlet.ResultActions saveDetails(MockHttpSession session) throws Exception {
        return mvc.perform(detailsRequest(session));
    }

    private MockHttpServletRequestBuilder detailsRequest(MockHttpSession session) {
        return post("/bookings/new/details").session(session).param("roomId", "2").param("date", "2027-01-12")
                .param("endDate", "2027-01-13").param("startTime", "09:00").param("endTime", "11:00")
                .param("purpose", " Team workshop ");
    }

    private MockHttpSession readyDraft() throws Exception {
        MockHttpSession session = new MockHttpSession();
        saveDetails(session).andExpect(status().is3xxRedirection());
        mvc.perform(post("/bookings/new/equipment").session(session).param("quantities[7]", "1"))
                .andExpect(redirectedUrl("/bookings/new/review"));
        return session;
    }

    private BookingResponse booking(Long id, Long userId, BookingStatus status) {
        return new BookingResponse(id, 2L, "Focus room", userId, "narin",
                LocalDateTime.of(2027, 1, 12, 9, 0), LocalDateTime.of(2027, 1, 13, 11, 0),
                status, "Team workshop", List.of(new BookingResponse.EquipmentItem(7L, "Projector", 1)),
                LocalDateTime.of(2026, 10, 6, 13, 0));
    }
}
