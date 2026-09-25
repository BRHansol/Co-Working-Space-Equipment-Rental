package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import com.example.roombooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final BookingStatusHistoryRepository historyRepository;

    public NotificationServiceImpl(BookingStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public void notifyBookingStatusChanged(Booking booking, BookingStatus oldStatus,
                                            BookingStatus newStatus, User changedBy) {
        // 1. บันทึกประวัติการเปลี่ยนสถานะลง booking_status_history
        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(booking);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        historyRepository.save(history);

        // 2. ส่งการแจ้งเตือน
        // ตอนนี้ยังไม่มีระบบอีเมล/SMS จริง เลย log ไว้ก่อนเป็น placeholder
        // ถ้าจะต่อยอด สามารถเพิ่ม EmailSender / push notification ตรงนี้ได้เลย
        // โดยไม่ต้องแก้ NotificationListener หรือจุดที่ publish event เลย
        log.info("แจ้งเตือน: booking #{} เปลี่ยนสถานะจาก {} เป็น {} โดย {}",
                booking.getId(),
                oldStatus,
                newStatus,
                changedBy != null ? changedBy.getUsername() : "system");
    }
}