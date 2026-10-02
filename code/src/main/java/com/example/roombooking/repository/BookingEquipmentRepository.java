package com.example.roombooking.repository;

import com.example.roombooking.domain.entity.BookingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {

    /**
     * รวมจำนวนอุปกรณ์ชิ้นนี้ (equipmentId) ที่ถูกจองไปแล้วในการจองที่ "ยังมีผลอยู่"
     * (สถานะ PENDING หรือ APPROVED เท่านั้น — CANCELLED/REJECTED/COMPLETED ไม่นับ)
     * และช่วงเวลาทับซ้อนกับ [startTime, endTime) ที่ขอมา
     *
     * ใช้ใน EquipmentAvailabilityHandler เพื่อคำนวณจำนวนคงเหลือ:
     * remaining = equipment.getTotalQuantity() - sumReservedQuantity(...)
     *
     * @param excludeBookingId ใช้ตัดการจองตัวเองออกตอนแก้ไขการจองเดิม ส่ง null
     *                         ได้ตอนสร้างใหม่
     * @return ผลรวมจำนวนที่จองไว้แล้ว (0 ถ้าไม่มีรายการใดตรงเงื่อนไข ไม่คืน null
     *         เพราะใช้ COALESCE)
     */
    @Query("""
            SELECT COALESCE(SUM(be.quantity), 0)
            FROM BookingEquipment be
            JOIN be.booking b
            WHERE be.equipment.id = :equipmentId
              AND b.status IN (com.example.roombooking.domain.enums.BookingStatus.PENDING,
                               com.example.roombooking.domain.enums.BookingStatus.APPROVED)
              AND b.startTime < :endTime
              AND b.endTime > :startTime
              AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
            """)
    Long sumReservedQuantity(
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
}
