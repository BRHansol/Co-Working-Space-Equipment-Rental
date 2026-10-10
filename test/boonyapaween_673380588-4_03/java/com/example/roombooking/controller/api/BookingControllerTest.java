package com.example.roombooking.controller.api;

import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import com.example.roombooking.exception.InvalidStateTransitionException;
import com.example.roombooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST layer of Booking: URL mapping, HTTP status codes, X-User-Id header
 * forwarding and pagination/sorting parameters.
 * Error-to-status mapping is covered in detail by GlobalExceptionHandlerTest.
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String BOOKING_JSON = """
            {"roomId": 2, "startTime": "2030-01-01T09:00:00", "endTime": "2030-01-01T11:00:00", "purpose": "Demo"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private static BookingResponse response(Long id, BookingStatus status) {
        return BookingResponse.builder()
                .id(id)
                .roomId(2L)
                .roomName("VIP Room")
                .userId(1L)
                .username("sol")
                .startTime(LocalDateTime.of(2030, 1, 1, 9, 0))
                .endTime(LocalDateTime.of(2030, 1, 1, 11, 0))
                .status(status)
                .purpose("Demo")
                .equipmentItems(List.of())
                .build();
    }

    @Test
    void createBooking_returns201_andForwardsRequesterHeader() throws Exception {
        when(bookingService.createBooking(any(BookingCreateRequest.class), eq(1L)))
                .thenReturn(response(100L, BookingStatus.PENDING));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BOOKING_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomName").value("VIP Room"));

        ArgumentCaptor<BookingCreateRequest> captor = ArgumentCaptor.forClass(BookingCreateRequest.class);
        verify(bookingService).createBooking(captor.capture(), eq(1L));
        assertThat(captor.getValue().getRoomId()).isEqualTo(2L);
        assertThat(captor.getValue().getPurpose()).isEqualTo("Demo");
    }

    @Test
    void getBooking_returns200WithBody() throws Exception {
        when(bookingService.getBookingById(100L)).thenReturn(response(100L, BookingStatus.APPROVED));

        mockMvc.perform(get("/api/v1/bookings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.username").value("sol"));
    }

    @Test
    void getBookingsByRoom_withoutParams_usesDefaultPageSizeAndSort() throws Exception {
        when(bookingService.getBookingsByRoom(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(100L, BookingStatus.PENDING))));

        mockMvc.perform(get("/api/v1/rooms/3/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingService).getBookingsByRoom(eq(3L), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("startTime")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("startTime").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getBookingsByRoom_readsPageSizeAndSortFromQuery() throws Exception {
        when(bookingService.getBookingsByRoom(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/rooms/3/bookings")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "startTime,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingService).getBookingsByRoom(eq(3L), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("startTime").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getBookingsByUser_returnsPageOfThatUser() throws Exception {
        when(bookingService.getBookingsByUser(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(100L, BookingStatus.PENDING),
                        response(101L, BookingStatus.CANCELLED))));

        mockMvc.perform(get("/api/v1/users/1/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].status").value("CANCELLED"));
    }

    @Test
    void updateBooking_returns200_andForwardsIdAndRequester() throws Exception {
        when(bookingService.updateBooking(eq(100L), any(BookingCreateRequest.class), eq(1L)))
                .thenReturn(response(100L, BookingStatus.PENDING));

        mockMvc.perform(put("/api/v1/bookings/100")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BOOKING_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(bookingService).updateBooking(eq(100L), any(BookingCreateRequest.class), eq(1L));
    }

    @Test
    void updateStatus_forwardsTargetStatusAndActor() throws Exception {
        when(bookingService.updateStatus(100L, BookingStatus.APPROVED, 7L))
                .thenReturn(response(100L, BookingStatus.APPROVED));

        mockMvc.perform(patch("/api/v1/bookings/100/status")
                        .header("X-User-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).updateStatus(100L, BookingStatus.APPROVED, 7L);
    }

    @Test
    void updateStatus_illegalTransition_returns409() throws Exception {
        when(bookingService.updateStatus(100L, BookingStatus.CANCELLED, 1L))
                .thenThrow(new InvalidStateTransitionException(BookingStatus.COMPLETED, "cancel"));

        mockMvc.perform(patch("/api/v1/bookings/100/status")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot cancel a booking in status COMPLETED"));
    }
}