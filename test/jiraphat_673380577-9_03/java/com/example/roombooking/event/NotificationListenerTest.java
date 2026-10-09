package com.example.roombooking.event;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationListener listener;

    @Test
    void handleBookingStatusChange_forwardsEveryEventFieldToNotificationService() {
        Booking booking = Booking.builder().id(10L).status(BookingStatus.APPROVED).build();
        User admin = new User();
        admin.setId(1L);
        BookingStatusChangedEvent event = new BookingStatusChangedEvent(
                this, booking, BookingStatus.PENDING, BookingStatus.APPROVED, admin);

        listener.handleBookingStatusChange(event);

        verify(notificationService).notifyBookingStatusChanged(
                booking, BookingStatus.PENDING, BookingStatus.APPROVED, admin);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void handleBookingStatusChange_systemChangeWithoutActor_passesNullChangedBy() {
        Booking booking = Booking.builder().id(11L).status(BookingStatus.COMPLETED).build();
        BookingStatusChangedEvent event = new BookingStatusChangedEvent(
                this, booking, BookingStatus.APPROVED, BookingStatus.COMPLETED, null);

        listener.handleBookingStatusChange(event);

        verify(notificationService).notifyBookingStatusChanged(
                booking, BookingStatus.APPROVED, BookingStatus.COMPLETED, null);
    }
}
