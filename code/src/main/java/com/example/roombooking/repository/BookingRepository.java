package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByRoomId(Long roomId, Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    boolean existsByRoomIdAndStatusNotInAndStartTimeBeforeAndEndTimeAfter(
            Long roomId,
            Collection<BookingStatus> excluded,
            LocalDateTime endTime,
            LocalDateTime startTime
    );
}