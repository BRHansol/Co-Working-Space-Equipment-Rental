package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.exception.InvalidStateTransitionException;

public interface BookingState {

    BookingStatus getStatus();

    default void approve(BookingContext context) {
        throw invalid("approve");
    }

    default void reject(BookingContext context) {
        throw invalid("reject");
    }

    default void cancel(BookingContext context) {
        throw invalid("cancel");
    }

    default void complete(BookingContext context) {
        throw invalid("complete");
    }

    private InvalidStateTransitionException invalid(String action) {
        return new InvalidStateTransitionException(getStatus(), action);
    }
}