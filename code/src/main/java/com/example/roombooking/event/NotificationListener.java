package com.example.roombooking.event;

import com.example.roombooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
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