package com.example.roombooking.event;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
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
}