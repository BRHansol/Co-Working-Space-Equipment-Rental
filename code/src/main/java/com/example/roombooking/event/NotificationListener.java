package com.example.roombooking.event;

import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    // 1. ประกาศ Logger ด้วยตัวเอง
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final BookingStatusHistoryRepository historyRepository;

    // 2. สร้าง Constructor เองเพื่อ Inject dependency
    public NotificationListener(BookingStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @EventListener
    @Async
    public void handleBookingStatusChange(BookingStatusChangedEvent event) {
        log.info("Booking {} status changed from {} to {}", 
                 event.getBooking().getId(), event.getOldStatus(), event.getNewStatus());

        // 3. ใช้ new แล้ว Set ค่าแทนการใช้ .builder()
        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(event.getBooking());
        history.setOldStatus(event.getOldStatus());
        history.setNewStatus(event.getNewStatus());
        history.setChangedBy(event.getChangedBy());
        
        historyRepository.save(history);
    }
}