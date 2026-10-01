package com.example.roombooking.service;

import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request);

    BookingResponse getBookingById(Long id);

    Page<BookingResponse> getBookingsByRoom(Long roomId, Pageable pageable);

    Page<BookingResponse> getBookingsByUser(Long userId, Pageable pageable);

    BookingResponse approveBooking(Long id);

    BookingResponse rejectBooking(Long id);

    BookingResponse cancelBooking(Long id);

    BookingResponse completeBooking(Long id);
}