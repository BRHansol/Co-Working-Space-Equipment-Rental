package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private BookingStatusHistoryRepository historyRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notifyBookingStatusChanged_savesHistoryWithAllFields() {
        Booking booking = Booking.builder().id(5L).status(BookingStatus.REJECTED).build();
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");

        notificationService.notifyBookingStatusChanged(
                booking, BookingStatus.PENDING, BookingStatus.REJECTED, admin);

        ArgumentCaptor<BookingStatusHistory> captor = ArgumentCaptor.forClass(BookingStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        BookingStatusHistory saved = captor.getValue();
        assertThat(saved.getBooking()).isSameAs(booking);
        assertThat(saved.getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getNewStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(saved.getChangedBy()).isSameAs(admin);
    }

    @Test
    void notifyBookingStatusChanged_withoutActor_stillSavesHistory() {
        Booking booking = Booking.builder().id(6L).status(BookingStatus.COMPLETED).build();

        notificationService.notifyBookingStatusChanged(
                booking, BookingStatus.APPROVED, BookingStatus.COMPLETED, null);

        ArgumentCaptor<BookingStatusHistory> captor = ArgumentCaptor.forClass(BookingStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNull();
        assertThat(captor.getValue().getNewStatus()).isEqualTo(BookingStatus.COMPLETED);
    }
}
