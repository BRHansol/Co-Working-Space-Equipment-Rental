package com.example.roombooking.service.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.exception.RoomNotAvailableException;
import com.example.roombooking.repository.MeetingRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RoomAvailabilityHandler extends BookingValidationHandler{
    private final MeetingRoomRepository meetingRoomRepository;

    @Override
    protected void doValidate(BookingValidationContext context) {
        Long roomId = context.getRequest().getRoomId();
        log.debug("[Validation] ตรวจสอบสถานะห้อง roomId={}", roomId);
 
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบห้องประชุม id=" + roomId));
 
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomNotAvailableException(
                    "ห้อง '" + room.getName() + "' ไม่พร้อมให้บริการในขณะนี้ (สถานะ: " + room.getStatus() + ")");
        }
 
        context.setRoom(room);
}
