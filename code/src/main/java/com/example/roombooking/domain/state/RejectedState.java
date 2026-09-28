package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public class RejectedState implements BookingState {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.REJECTED;
    }
}