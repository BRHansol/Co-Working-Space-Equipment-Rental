package com.example.roombooking.event;

import com.example.roombooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Observer: ฟัง BookingStatusChangedEvent แล้วส่งต่อให้ NotificationService
// (บันทึก history + แจ้งเตือน) โดย BookingService ไม่ต้องรู้จัก listener ตัวนี้เลย
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // AFTER_COMMIT: ทำงานหลังการเปลี่ยนสถานะ commit ลง DB แล้วเท่านั้น
    // ถ้า transaction ของ booking rollback จะไม่มี history/แจ้งเตือนหลุดออกไป
    // fallbackExecution: ถ้า publish นอก transaction ก็ยังทำงาน
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleBookingStatusChange(BookingStatusChangedEvent event) {
        log.info("Booking {} status changed from {} to {}",
                 event.getBooking().getId(), event.getOldStatus(), event.getNewStatus());

        notificationService.notifyBookingStatusChanged(
                event.getBooking(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getChangedBy());
    }
}
