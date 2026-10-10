package com.example.roombooking.controller.api;

import com.example.roombooking.dto.response.BookingStatusHistoryResponse;
import com.example.roombooking.service.BookingStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// อ่านผลของ Observer: ทุกครั้งที่ updateStatus สำเร็จ NotificationListener จะบันทึกแถวใหม่ไว้ให้
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Booking Status History", description = "ประวัติการเปลี่ยนสถานะการจอง (บันทึกโดย Observer)")
public class BookingStatusHistoryController {

    private final BookingStatusHistoryService historyService;

    public BookingStatusHistoryController(BookingStatusHistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(summary = "ดูประวัติการเปลี่ยนสถานะของการจอง เรียงจากล่าสุด")
    @GetMapping("/bookings/{id}/status-history")
    public ResponseEntity<List<BookingStatusHistoryResponse>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(historyService.getHistory(id));
    }
}
