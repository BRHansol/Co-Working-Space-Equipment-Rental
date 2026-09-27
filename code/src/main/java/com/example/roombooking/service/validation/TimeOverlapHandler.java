package com.example.roombooking.service.validation;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.exception.RoomNotAvailableException;
import com.example.roombooking.repository.BookingRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(3)
public class TimeOverlapHandler extends BookingValidationHandler{
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED);
 
    private final BookingRepository bookingRepository;
 
    @Override
    protected void doValidate(BookingValidationContext context) {
        BookingCreateRequest request = context.getRequest();
 
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("เวลาเริ่มต้นต้องอยู่ก่อนเวลาสิ้นสุด");
        }
 
        log.debug("[Validation] ตรวจสอบเวลาซ้อนทับ roomId={} ช่วง {} - {}",
                request.getRoomId(), request.getStartTime(), request.getEndTime());
 
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime(),
                ACTIVE_STATUSES);
 
        if (request.getBookingId() != null) {
            // กรณีแก้ไขการจองเดิม ต้องตัดตัวเองออกจากผลลัพธ์ ไม่งั้นจะชนกับตัวเอง
            overlapping = overlapping.stream()
                    .filter(b -> !b.getId().equals(request.getBookingId()))
                    .toList();
        }
 
        if (!overlapping.isEmpty()) {
            throw new RoomNotAvailableException("ห้องประชุมถูกจองแล้วในช่วงเวลาที่เลือก กรุณาเลือกเวลาอื่น");
        }
    }
}
