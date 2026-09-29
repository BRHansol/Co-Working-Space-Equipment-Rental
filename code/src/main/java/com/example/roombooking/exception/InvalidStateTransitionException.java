package com.example.roombooking.exception;

import com.example.roombooking.domain.enums.BookingStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(BookingStatus from, String action) {
        super("Cannot " + action + " a booking in status " + from);
    }
}