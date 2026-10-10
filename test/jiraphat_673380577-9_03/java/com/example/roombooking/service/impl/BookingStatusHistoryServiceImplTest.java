package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.BookingStatusHistory;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.response.BookingStatusHistoryResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.BookingStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class BookingStatusHistoryServiceImplTest {

    @Mock
    private BookingStatusHistoryRepository historyRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingStatusHistoryServiceImpl historyService;

    @Test
    void getHistory_mapsEveryFieldIncludingWhoChangedIt() {
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin01");
        LocalDateTime changedAt = LocalDateTime.of(2026, 10, 11, 16, 20);
        BookingStatusHistory row = history(10L, BookingStatus.PENDING, BookingStatus.APPROVED, admin, changedAt);
        when(bookingRepository.existsById(5L)).thenReturn(true);
        when(historyRepository.findByBooking_IdOrderByChangedAtDesc(5L)).thenReturn(List.of(row));

        List<BookingStatusHistoryResponse> result = historyService.getHistory(5L);

        assertThat(result).hasSize(1);
        BookingStatusHistoryResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getBookingId()).isEqualTo(5L);
        assertThat(response.getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getNewStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(response.getChangedById()).isEqualTo(2L);
        assertThat(response.getChangedBy()).isEqualTo("admin01");
        assertThat(response.getChangedAt()).isEqualTo(changedAt);
    }

    @Test
    void getHistory_changeWithoutActor_isShownAsSystem() {
        when(bookingRepository.existsById(5L)).thenReturn(true);
        when(historyRepository.findByBooking_IdOrderByChangedAtDesc(5L)).thenReturn(List.of(
                history(11L, BookingStatus.APPROVED, BookingStatus.COMPLETED, null, LocalDateTime.now())));

        BookingStatusHistoryResponse response = historyService.getHistory(5L).get(0);

        assertThat(response.getChangedById()).isNull();
        assertThat(response.getChangedBy()).isEqualTo("system");
    }

    @Test
    void getHistory_bookingWithoutChanges_returnsEmptyList() {
        when(bookingRepository.existsById(5L)).thenReturn(true);
        when(historyRepository.findByBooking_IdOrderByChangedAtDesc(5L)).thenReturn(List.of());

        assertThat(historyService.getHistory(5L)).isEmpty();
    }

    @Test
    void getHistory_unknownBooking_throwsNotFound() {
        when(bookingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> historyService.getHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(historyRepository, never()).findByBooking_IdOrderByChangedAtDesc(anyLong());
    }

    private static BookingStatusHistory history(Long id, BookingStatus oldStatus, BookingStatus newStatus,
                                                User changedBy, LocalDateTime changedAt) {
        BookingStatusHistory history = new BookingStatusHistory();
        history.setId(id);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        return history;
    }
}
