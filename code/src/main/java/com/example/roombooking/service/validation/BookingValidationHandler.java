package com.example.roombooking.service.validation;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.BookingCreateRequest;

public abstract class BookingValidationHandler {
    protected final void handle(BookingValidationContext context) {
        doValidate(context);
    }

    protected abstract void doValidate(BookingValidationContext context);

    protected final BookingCreateRequest getRequest(BookingValidationContext context) {
        return context.getRequest();
    }

    protected final User getRequester(BookingValidationContext context) {
        return context.getRequester();
    }

    protected final MeetingRoom getRoom(BookingValidationContext context) {
        return context.getRoom();
    }

    protected final void setRoom(BookingValidationContext context, MeetingRoom room) {
        context.setRoom(room);
    }
}
