package com.example.roombooking.service.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingValidationChain {
        // Spring inject handler ทุกตัวมาให้ และเรียงตาม @Order แล้ว
    private final List<BookingValidationHandler> handlers;

    public void validate(BookingValidationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("กรุณาระบุข้อมูลสำหรับตรวจสอบการจอง");
        }
        for (BookingValidationHandler handler : handlers) {
            handler.handle(context);
        }
    }

}
