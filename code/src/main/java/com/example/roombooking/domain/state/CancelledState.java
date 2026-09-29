package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public class CancelledState implements BookingState {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.CANCELLED;
    }
}