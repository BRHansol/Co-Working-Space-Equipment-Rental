package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.response.BookingStatusHistoryResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import com.example.roombooking.service.BookingStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingStatusHistoryServiceImpl implements BookingStatusHistoryService {

    private final BookingStatusHistoryRepository historyRepository;
    private final BookingRepository bookingRepository;

    public BookingStatusHistoryServiceImpl(BookingStatusHistoryRepository historyRepository,
                                           BookingRepository bookingRepository) {
        this.historyRepository = historyRepository;
        this.bookingRepository = bookingRepository;
    }

    // readOnly transaction: changedBy เป็น LAZY และปิด open-in-view ไว้ ต้องแปลงเป็น DTO ให้เสร็จในนี้
    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getHistory(Long bookingId) {
        // แยก "ไม่มี booking นี้" (404) ออกจาก "มี booking แต่ยังไม่เคยเปลี่ยนสถานะ" (200 + [])
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking not found: id=" + bookingId);
        }
        return historyRepository.findByBooking_IdOrderByChangedAtDesc(bookingId).stream()
                .map(history -> toResponse(history, bookingId))
                .toList();
    }

    private BookingStatusHistoryResponse toResponse(BookingStatusHistory history, Long bookingId) {
        BookingStatusHistoryResponse response = new BookingStatusHistoryResponse();
        response.setId(history.getId());
        response.setBookingId(bookingId);
        response.setOldStatus(history.getOldStatus());
        response.setNewStatus(history.getNewStatus());
        User changedBy = history.getChangedBy();
        response.setChangedById(changedBy != null ? changedBy.getId() : null);
        response.setChangedBy(changedBy != null ? changedBy.getUsername() : "system");
        response.setChangedAt(history.getChangedAt());
        return response;
    }
}
