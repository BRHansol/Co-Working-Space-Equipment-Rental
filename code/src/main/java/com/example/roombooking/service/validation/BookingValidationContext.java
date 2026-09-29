package com.example.roombooking.service.validation;

import java.util.HashMap;
import java.util.Map;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.BookingCreateRequest;

public class BookingValidationContext {

    private final BookingCreateRequest request;
    private final User requester;

    // ข้อมูลที่ Handler ระหว่างทางจะนำมาเก็บ
    private MeetingRoom room;

    // เก็บจำนวนอุปกรณ์ที่ต้องการ
    private final Map<Long, Integer> requestedEquipmentQuantities = new HashMap<>();

    public BookingValidationContext(
            BookingCreateRequest request,
            User requester) {

        this.request = request;
        this.requester = requester;
    }

    public BookingCreateRequest getRequest() {
        return request;
    }

    public User getRequester() {
        return requester;
    }

    public MeetingRoom getRoom() {
        return room;
    }

    public void setRoom(MeetingRoom room) {
        this.room = room;
    }

    public Map<Long, Integer> getRequestedEquipmentQuantities() {
        return requestedEquipmentQuantities;
    }
}