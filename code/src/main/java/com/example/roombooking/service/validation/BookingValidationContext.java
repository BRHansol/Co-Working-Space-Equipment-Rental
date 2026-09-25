package com.example.roombooking.service.validation;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.BookingCreateRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingValidationContext {
    private final BookingCreateRequest request;
    private final User requester;
    private MeetingRoom room;

    public BookingValidationContext(BookingCreateRequest request, User requester) {
        this.request = request;
        this.requester = requester;
    }
}
