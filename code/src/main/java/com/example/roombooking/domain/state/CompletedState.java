package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public class CompletedState implements BookingState {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.COMPLETED;
    }
}