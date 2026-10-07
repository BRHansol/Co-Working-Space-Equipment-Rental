package com.example.roombooking.service.validation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingValidationChain {
    // Spring inject handler ทุกตัวมาให้ และเรียงตาม @Order แล้ว
    private final List<BookingValidationHandler> handlers;

    public BookingValidationChain(List<BookingValidationHandler> handlers) {
        this.handlers = handlers;
    }

    public void validate(BookingValidationContext context) {
        for (BookingValidationHandler handler : handlers) {
            handler.handle(context);
        }
    }

}
