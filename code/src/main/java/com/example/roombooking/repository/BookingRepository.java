package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByRoomId(Long roomId, Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status NOT IN ('CANCELLED', 'REJECTED')
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findOverlapping(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}