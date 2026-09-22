package com.example.roombooking.service.validation;

import java.util.HashMap;
import java.util.Map;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.BookingCreateRequest;

public class BookingValidationHandler {
    private final BookingCreateRequest request;
    private final User requester;
 
    // ค่าที่ resolve ระหว่างทาง (populate โดย handler ที่เกี่ยวข้อง)
    private MeetingRoom room;
    private final Map<Long, Integer> requestedEquipmentQuantities = new HashMap<>();
 
    public BookingValidationContext(BookingCreateRequest request, User requester) {
        this.request = request;
        this.requester = requester;
    }
    
}
