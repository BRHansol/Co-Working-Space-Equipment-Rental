package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public class ApprovedState implements BookingState {

    @Override
    public void cancel(BookingContext context) {
        context.setState(new CancelledState());
    }

    @Override
    public void complete(BookingContext context) {
        context.setState(new CompletedState());
    }

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.APPROVED;
    }
}