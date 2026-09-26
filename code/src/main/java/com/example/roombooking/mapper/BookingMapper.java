package com.example.roombooking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;

@Component
public class BookingMapper {
    public Booking toEntity(BookingCreateRequest request, MeetingRoom room, User user) {
        return Booking.builder()
                .room(room)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .purpose(request.getPurpose())
                .status(BookingStatus.PENDING) // การจองใหม่เริ่มที่ PENDING เสมอ รอ approve
                .build();
    }
 
    /**
     * อัปเดตค่าของ Booking entity เดิมจาก request (ใช้ตอนแก้ไขการจอง)
     * ไม่แตะ status / user ตรงนี้ เพราะเป็นข้อมูลที่ไม่ควรเปลี่ยนหลังสร้างแล้ว
     */
    public void updateEntity(Booking booking, BookingCreateRequest request, MeetingRoom room) {
        booking.setRoom(room);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
    }
 
    public BookingResponse toResponse(Booking booking) {
        List<BookingResponse.EquipmentItem> equipmentItems =
                CollectionUtils.isEmpty(booking.getBookingEquipments())
                        ? List.of()
                        : booking.getBookingEquipments().stream()
                                .map(this::toEquipmentItem)
                                .toList();
 
        return BookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getName())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .purpose(booking.getPurpose())
                .equipmentItems(equipmentItems)
                .createdAt(booking.getCreatedAt())
                .build();
    }
 
    public List<BookingResponse> toResponseList(List<Booking> bookings) {
        return bookings.stream().map(this::toResponse).toList();
    }
 
    private BookingResponse.EquipmentItem toEquipmentItem(BookingEquipment bookingEquipment) {
        return BookingResponse.EquipmentItem.builder()
                .equipmentId(bookingEquipment.getEquipment().getId())
                .equipmentName(bookingEquipment.getEquipment().getName())
                .quantity(bookingEquipment.getQuantity())
                .build();
    }
}
