package com.example.roombooking.event;

import com.example.roombooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

// ใช้ @TransactionalEventListener(AFTER_COMMIT) แทน @EventListener ธรรมดา
// เพราะ event ถูกยิงจากกลาง @Transactional ของ BookingServiceImpl.updateStatus()
// ถ้าใช้ @EventListener เฉยๆ + @Async อาจรันคนละ thread ก่อน transaction commit จริง
// ทำให้ไปอ่าน booking ที่ยังไม่ commit ได้ หรือถ้า transaction rollback ก็ไม่ควรแจ้งเตือนเลย
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Async
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