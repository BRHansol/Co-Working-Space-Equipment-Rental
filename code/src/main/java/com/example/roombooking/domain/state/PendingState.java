package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public class PendingState implements BookingState {

    @Override
    public void approve(BookingContext context) {
        context.setState(new ApprovedState());
    }

    @Override
    public void reject(BookingContext context) {
        context.setState(new RejectedState());
    }

    @Override
    public void cancel(BookingContext context) {
        context.setState(new CancelledState());
    }

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.PENDING;
    }
}