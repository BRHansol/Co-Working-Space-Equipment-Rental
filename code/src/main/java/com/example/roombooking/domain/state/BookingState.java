package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.exception.InvalidStateTransitionException;

/*
 * Contract: every state handles all four actions.
 * An allowed transition moves the context to the next state;
 * a transition not allowed throws InvalidStateTransitionException.
 * Default methods reject every transition, so each state overrides
 * only the transitions it actually allows.
 */
public interface BookingState {

    BookingStatus getStatus();

    // Only states that override this allow the booking details to be edited.
    default boolean isEditable() {
        return false;
    }

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