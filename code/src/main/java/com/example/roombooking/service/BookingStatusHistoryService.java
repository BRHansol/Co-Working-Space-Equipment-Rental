package com.example.roombooking.service;

import com.example.roombooking.dto.response.BookingStatusHistoryResponse;

import java.util.List;

public interface BookingStatusHistoryService {

    // ประวัติการเปลี่ยนสถานะของ booking เรียงจากล่าสุด
    List<BookingStatusHistoryResponse> getHistory(Long bookingId);
}
