package com.example.roombooking.domain.state;

import com.example.roombooking.domain.enums.BookingStatus;

public interface BookingState {

    void approve(BookingContext context);

    void reject(BookingContext context);

    void cancel(BookingContext context);

    void complete(BookingContext context);

    BookingStatus getStatus();
}