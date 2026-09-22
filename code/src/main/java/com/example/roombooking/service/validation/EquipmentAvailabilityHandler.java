package com.example.roombooking.service.validation;

public class EquipmentAvailabilityHandler {
    private BookingValidationHandler next;

    public BookingValidationHandler setNext(BookingValidationHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(BookingValidationContext context) {
        doValidate(context);
        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void doValidate(BookingValidationContext context);
}
