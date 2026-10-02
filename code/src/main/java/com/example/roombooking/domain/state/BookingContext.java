package com.example.roombooking.domain.state;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.BookingStatus;

public class BookingContext {

    private final Booking booking;
    private BookingState state;

    public BookingContext(Booking booking) {
        this.booking = booking;
        this.state = resolveState(booking.getStatus());
    }

    public void approve() {
        state.approve(this);
    }

    public void reject() {
        state.reject(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    public void complete() {
        state.complete(this);
    }

    public boolean isEditable() {
        return state.isEditable();
    }

    // Called by concrete states after a valid transition; syncs back to the entity.
    void setState(BookingState newState) {
        this.state = newState;
        this.booking.setStatus(newState.getStatus());
    }

    public Booking getBooking() {
        return booking;
    }

    private static BookingState resolveState(BookingStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case APPROVED -> new ApprovedState();
            case REJECTED -> new RejectedState();
            case CANCELLED -> new CancelledState();
            case COMPLETED -> new CompletedState();
        };
    }
}