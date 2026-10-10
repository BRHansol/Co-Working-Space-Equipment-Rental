package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.BookingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {

    /**
     * หาจำนวนอุปกรณ์ชิ้นนี้ (equipmentId) ที่ถูกจองพร้อมกันสูงสุดในการจองที่ "ยังมีผลอยู่"
     * (สถานะ PENDING หรือ APPROVED เท่านั้น — CANCELLED/REJECTED/COMPLETED ไม่นับ)
     * และช่วงเวลาทับซ้อนกับ [startTime, endTime) ที่ขอมา
     *
     * ใช้ใน EquipmentAvailabilityHandler เพื่อคำนวณจำนวนคงเหลือ:
     * remaining = equipment.getTotalQuantity() - sumReservedQuantity(...)
     *
     * @param excludeBookingId ใช้ตัดการจองตัวเองออกตอนแก้ไขการจองเดิม ส่ง null
     *                         ได้ตอนสร้างใหม่
     * @return จำนวนที่จองพร้อมกันสูงสุดในช่วงที่ขอ (0 ถ้าไม่มีรายการ ไม่คืน null)
     */
    default Long sumReservedQuantity(Long equipmentId,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime,
                                     Long excludeBookingId) {
        Map<LocalDateTime, Long> quantityChanges = new TreeMap<>();
        for (ReservationWindow reservation : findOverlappingReservations(
                equipmentId, startTime, endTime, excludeBookingId)) {
            LocalDateTime start = reservation.getStartTime().isBefore(startTime)
                    ? startTime : reservation.getStartTime();
            LocalDateTime end = reservation.getEndTime().isAfter(endTime)
                    ? endTime : reservation.getEndTime();
            long quantity = reservation.getQuantity().longValue();

            // รวมการคืนและการยืมที่เวลาเดียวกันก่อนนับ เพื่อให้ช่วงเวลาเป็น [start, end)
            quantityChanges.merge(start, quantity, Long::sum);
            quantityChanges.merge(end, -quantity, Long::sum);
        }

        long reserved = 0;
        long peakReserved = 0;
        for (long change : quantityChanges.values()) {
            reserved += change;
            peakReserved = Math.max(peakReserved, reserved);
        }
        return peakReserved;
    }

    @Query("""
            SELECT b.startTime AS startTime, b.endTime AS endTime, be.quantity AS quantity
            FROM BookingEquipment be
            JOIN be.booking b
            WHERE be.equipment.id = :equipmentId
              AND b.status IN (com.example.roombooking.domain.enums.BookingStatus.PENDING,
                               com.example.roombooking.domain.enums.BookingStatus.APPROVED)
              AND b.startTime < :endTime
              AND b.endTime > :startTime
              AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
            """)
    List<ReservationWindow> findOverlappingReservations(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeBookingId") Long excludeBookingId);

    /**
     * ดึงรายการอุปกรณ์ทั้งหมดที่ผูกกับการจองเดียว ใช้ตอนแสดงผลหรือจัดการรายการของ
     * booking นั้น
     */
    List<BookingEquipment> findByBookingId(Long bookingId);

    /**
     * ลบอุปกรณ์ทั้งหมดของการจองหนึ่ง ๆ ใช้ตอนแก้ไขรายการอุปกรณ์ทั้งชุด
     * (ลบของเก่าทิ้งก่อนแล้วค่อยสร้างรายการใหม่ ง่ายกว่าการ diff ทีละรายการ)
     */
    void deleteByBookingId(Long bookingId);

    interface ReservationWindow {
        LocalDateTime getStartTime();

        LocalDateTime getEndTime();

        Integer getQuantity();
    }
}
