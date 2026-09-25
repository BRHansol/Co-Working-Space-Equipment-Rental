package com.example.roombooking.service;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;

public interface NotificationService {

    
    void notifyBookingStatusChanged(Booking booking, BookingStatus oldStatus,
                                     BookingStatus newStatus, User changedBy);
}

