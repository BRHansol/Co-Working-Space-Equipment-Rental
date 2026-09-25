package com.example.roombooking.dto.request;

import lombok.Data;

@Data
public class BookingCreateRequest {
    private Long roomId;
    private Long bookingForUserId;
}
