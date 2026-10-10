package com.example.roombooking.service;

import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request, Long requesterId);

    BookingResponse getBookingById(Long id);

    Page<BookingResponse> getBookingsByRoom(Long roomId, Pageable pageable);

    Page<BookingResponse> getBookingsByUser(Long userId, Pageable pageable);

    BookingResponse updateBooking(Long id, BookingCreateRequest request, Long requesterId);

    BookingResponse updateStatus(Long id, BookingStatus targetStatus, Long actorId);
}