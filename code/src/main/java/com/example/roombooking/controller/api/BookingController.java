package com.example.roombooking.controller.api;

import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.request.BookingStatusUpdateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import com.example.roombooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // TODO(teammate 1): take the user id from the logged-in user once SecurityConfig is done.
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, requesterId));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    // Example: GET /api/v1/rooms/3/bookings?page=0&size=10&sort=startTime,desc
    @GetMapping("/rooms/{roomId}/bookings")
    public ResponseEntity<Page<BookingResponse>> getBookingsByRoom(
            @PathVariable Long roomId,
            @PageableDefault(size = 10, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(bookingService.getBookingsByRoom(roomId, pageable));
    }

    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<Page<BookingResponse>> getBookingsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId, pageable));
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(bookingService.updateBooking(id, request, requesterId));
    }

    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ResponseEntity.ok(bookingService.updateStatus(id, request.getStatus(), actorId));
    }
}