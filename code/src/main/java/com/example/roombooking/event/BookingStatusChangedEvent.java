package com.example.roombooking.event;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import org.springframework.context.ApplicationEvent;

public class BookingStatusChangedEvent extends ApplicationEvent {
    
    private final Booking booking;
    private final BookingStatus oldStatus;
    private final BookingStatus newStatus;
    private final User changedBy;

    public BookingStatusChangedEvent(Object source, Booking booking, BookingStatus oldStatus, BookingStatus newStatus, User changedBy) {
        super(source);
        this.booking = booking;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }

    // --- Getters ---
    public Booking getBooking() {
        return booking;
    }

    public BookingStatus getOldStatus() {
        return oldStatus;
    }

    public BookingStatus getNewStatus() {
        return newStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }
}