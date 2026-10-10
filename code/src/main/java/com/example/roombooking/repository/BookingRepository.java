package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByRoomId(Long roomId, Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    // Derived query (no JPQL): existing.startTime < endTime AND existing.endTime > startTime
    List<Booking> findByRoomIdAndStatusInAndStartTimeBeforeAndEndTimeAfter(
            Long roomId,
            Collection<BookingStatus> statuses,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    // Used by TimeOverlapHandler; keeps its argument order (start before end).
    default List<Booking> findOverlappingBookings(Long roomId,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  Collection<BookingStatus> statuses) {
        return findByRoomIdAndStatusInAndStartTimeBeforeAndEndTimeAfter(roomId, statuses, endTime, startTime);
    }
}