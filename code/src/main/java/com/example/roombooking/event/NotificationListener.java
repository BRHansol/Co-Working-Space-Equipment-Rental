package com.example.roombooking.event;

import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final BookingStatusHistoryRepository historyRepository;
    

    @EventListener
    @Async 
    public void handleBookingStatusChange(BookingStatusChangedEvent event) {
        log.info("Booking {} status changed from {} to {}", 
                 event.getBooking().getId(), event.getOldStatus(), event.getNewStatus());

        // 1. บันทึก History ลง DB
        BookingStatusHistory history = BookingStatusHistory.builder()
                .booking(event.getBooking())
                .oldStatus(event.getOldStatus())
                .newStatus(event.getNewStatus())
                .changedBy(event.getChangedBy())
                .build();
        
        historyRepository.save(history);

        // 2. เรียกใช้ Notification Service (เช่น ส่ง Email หา user ว่าการจองอนุมัติแล้ว)
        // notificationService.sendBookingStatusEmail(event.getBooking(), event.getNewStatus());
    }
}
